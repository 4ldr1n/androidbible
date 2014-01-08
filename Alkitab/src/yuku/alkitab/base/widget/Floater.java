package yuku.alkitab.base.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.HapticFeedbackConstants;
import android.view.View;
import yuku.alkitab.util.Ari;
import yuku.alkitab.model.Book;
import yuku.alkitab.model.Version;

public class Floater extends View {
	public static final String TAG = Floater.class.getSimpleName();
	public static final int LONG_PRESS_DELAY_MILLIS = 650;

	enum State {
		idle,
		selectBook,
		selectChapter,
		selectVerse,
	}

	public interface Listener {
		void onSelectComplete(int ari);
	}

	Paint passivePaint;
	Paint activePaint;
	Paint activeBoxPaint;
	Paint.FontMetrics passiveFontMetrics;
	Paint.FontMetrics activeFontMetrics;
	Paint separatorPaint;
	Book[] books;
	int grid_columns;
	int grid_rows;
	int activeBookIndex = -1;
	int longPressBookIndex = -1;
	int previousActiveBookIndex;
	int activeChapterIndex = -1;
	int longPressChapterIndex = -1;
	int previousActiveChapterIndex;
	int activeVerseIndex = -1;
	float density;
	State state;
	Listener listener;

	Runnable checkLongPressBook = new Runnable() {
		@Override
		public void run() {
			if (activeBookIndex == longPressBookIndex) {
				// we have been at the same bookIndex since a few ms ago!
				performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
				commitBook();
			}
		}
	};

	Runnable checkLongPressChapter = new Runnable() {
		@Override
		public void run() {
			if (activeChapterIndex == longPressChapterIndex) {
				// we have been at the same chapterIndex since a few ms ago!
				performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
				commitChapter();
			}
		}
	};

	public Floater(final Context context) {
		super(context);
		init();
	}

	public Floater(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Floater(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		density = getResources().getDisplayMetrics().density;

		passivePaint = new Paint();
		passivePaint.setAntiAlias(true);
		passivePaint.setColor(0xffd0d0d0);
		passivePaint.setShadowLayer(2 * density, 0, 0, 0xff000000);

		activePaint = new Paint();
		activePaint.setAntiAlias(true);
		activePaint.setColor(0xffffffff);
		activePaint.setTypeface(Typeface.DEFAULT_BOLD);

		activeBoxPaint = new Paint();
		activeBoxPaint.setColor(0xff247c94);
		activeBoxPaint.setStyle(Paint.Style.FILL_AND_STROKE);

		separatorPaint = new Paint();
		separatorPaint.setStyle(Paint.Style.STROKE);
		separatorPaint.setColor(0xffd0d0d0);
		separatorPaint.setStrokeWidth(1 * density);

		passiveFontMetrics = new Paint.FontMetrics();
		activeFontMetrics = new Paint.FontMetrics();
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		final float w = getWidth() - getPaddingLeft() - getPaddingRight();
		final float h = getHeight() - getPaddingTop() - getPaddingBottom();

		if (books == null) return;
		canvas.drawColor(0xd0000000);

		if (state == State.selectBook) {
			final int book_count = books.length;

			if (book_count <= 33) { // e.g. NT only
				grid_columns = 1;
				grid_rows = 33;
			} else if (book_count <= 66) {
				grid_columns = 2;
				grid_rows = 33;
			} else {
				grid_columns = 3;
				grid_rows = (int) FloatMath.ceil((float) book_count / grid_columns);
			}

			initFontSizes(h);

			// passive books
			for (int i = 0; i < book_count; i++) {
				final Book book = books[i];
				final int column = i / grid_rows;
				final int row = i % grid_rows;
				final float left = getPaddingLeft() + column * (w / grid_columns);
				final float bottom = getPaddingTop() + (row + 1) * (h / grid_rows);

				if (activeBookIndex != i) {
					canvas.drawText(book.shortName, left, bottom, passivePaint);
				}

				if (i == 38) { // end of old testament
					canvas.drawLine(left - 4 * density, bottom + passiveFontMetrics.descent, left + (w / grid_columns) - 4 * density, bottom + passiveFontMetrics.descent, separatorPaint);
				}
			}

			// active book
			if (activeBookIndex != -1) {
				final int column = activeBookIndex / grid_rows;
				final int row = activeBookIndex % grid_rows;
				final Book book = books[activeBookIndex];

				// draw box
				final String text = book.shortName;
				drawActiveWithBox(canvas, w, h, column, row, text);
			}
		}

		if (state == State.selectChapter) {
			final Book book = books[activeBookIndex];
			final int chapter_count = book.chapter_count;

			if (chapter_count <= 33) {
				grid_columns = 1;
				grid_rows = 33;
			} else if (chapter_count <= 66) {
				grid_columns = 2;
				grid_rows = 33;
			} else {
				grid_columns = 4;
				grid_rows = (int) FloatMath.ceil((float) chapter_count / grid_columns);
			}

			initFontSizes(h);

			final String prefix = grid_columns > 2? "": (book.shortName + " ");

			// passive chapters
			for (int i = 0; i < chapter_count; i++) {
				if (activeChapterIndex != i) {
					final int column = i / grid_rows;
					final int row = i % grid_rows;
					canvas.drawText(prefix + (i + 1), getPaddingLeft() + column * (w / grid_columns), getPaddingTop() + (row + 1) * (h / grid_rows), passivePaint);
				}
			}

			// active chapter
			if (activeChapterIndex != -1) {
				final int column = activeChapterIndex / grid_rows;
				final int row = activeChapterIndex % grid_rows;
				drawActiveWithBox(canvas, w, h, column, row, prefix + (activeChapterIndex + 1));
			}
		}

		if (state == State.selectVerse) {
			final Book book = books[activeBookIndex];
			final int verse_count = book.verse_counts[activeChapterIndex];

			if (verse_count <= 33) {
				grid_columns = 1;
				grid_rows = 33;
			} else if (verse_count <= 66) {
				grid_columns = 2;
				grid_rows = 33;
			} else {
				grid_columns = 4;
				grid_rows = (int) FloatMath.ceil((float) verse_count / grid_columns);
			}

			initFontSizes(h);

			final String prefix = grid_columns > 2? ((activeChapterIndex + 1) + ":"): (book.shortName + " " + (activeChapterIndex + 1) + ":");

			// passive verses
			for (int i = 0; i < verse_count; i++) {
				if (activeVerseIndex != i) {
					final int column = i / grid_rows;
					final int row = i % grid_rows;
					canvas.drawText(prefix + (i + 1), getPaddingLeft() + column * (w / grid_columns), getPaddingTop() + (row + 1) * (h / grid_rows), passivePaint);
				}
			}

			// active verse
			if (activeVerseIndex != -1) {
				final int column = activeVerseIndex / grid_rows;
				final int row = activeVerseIndex % grid_rows;
				drawActiveWithBox(canvas, w, h, column, row, prefix + (activeVerseIndex + 1));
			}
		}
	}

	private void initFontSizes(final float h) {
		passivePaint.setTextSize(0.9f * h / grid_rows);
		activePaint.setTextSize(h / grid_rows);
		passivePaint.getFontMetrics(passiveFontMetrics);
		activePaint.getFontMetrics(activeFontMetrics);
	}

	private void drawActiveWithBox(final Canvas canvas, final float w, final float h, final int column, final int row, final String text) {
		final float textWidth = activePaint.measureText(text);

		final float left = getPaddingLeft() + column * (w / grid_columns);
		final float bottom = getPaddingTop() + (row + 1) * (h / grid_rows);

		final float bleed = 4 * density;
		canvas.drawRect(left - bleed, bottom + activeFontMetrics.ascent - bleed, left + textWidth + bleed, bottom + activeFontMetrics.descent + bleed, activeBoxPaint);

		// draw text
		canvas.drawText(text, left, bottom, activePaint);
	}

	public void onDragStart(final Version version) {
		this.books = version.getConsecutiveBooks();
		this.state = State.selectBook;
		this.activeBookIndex = -1;
		this.activeChapterIndex = -1;
		this.activeVerseIndex = -1;
	}

	public void onDragMove(final float px, final float py) {
		if (books == null) return;

		final float x = px - getPaddingLeft();
		final float y = py - getPaddingTop();

		final float w = getWidth() - getPaddingLeft() - getPaddingRight();
		final float h = getHeight() - getPaddingTop() - getPaddingBottom();

		final int column = (int) (x / w * grid_columns);
		final int row = (int) (y / h * grid_rows);

		final int itemIndex = (y < 0 || y > h)? -1: (column * grid_rows + row);

		if (state == State.selectBook) {
			// check for invalid book index
			if (itemIndex < 0 || itemIndex >= books.length) {
				activeBookIndex = -1;
			} else {
				activeBookIndex = itemIndex;
			}

			if (activeBookIndex != previousActiveBookIndex || activeBookIndex == -1) {
				removeCallbacks(checkLongPressBook);

				if (activeBookIndex != -1) {
					longPressBookIndex = activeBookIndex;
					postDelayed(checkLongPressBook, LONG_PRESS_DELAY_MILLIS);
				}
			}

			previousActiveBookIndex = activeBookIndex;
		}

		if (state == State.selectChapter) {
			final Book book = books[activeBookIndex];
			final int chapter_count = book.chapter_count;

			if (itemIndex < 0 || itemIndex >= chapter_count) {
				activeChapterIndex = -1;
			} else {
				activeChapterIndex = itemIndex;
			}

			if (activeChapterIndex != previousActiveChapterIndex || activeChapterIndex == -1) {
				removeCallbacks(checkLongPressChapter);

				if (activeChapterIndex != -1) {
					longPressChapterIndex = activeChapterIndex;
					postDelayed(checkLongPressChapter, LONG_PRESS_DELAY_MILLIS);
				}
			}

			previousActiveChapterIndex = activeChapterIndex;
		}

		if (state == State.selectVerse) {
			final Book book = books[activeBookIndex];
			final int verse_count = book.verse_counts[activeChapterIndex];

			if (itemIndex < 0 || itemIndex >= verse_count) {
				activeVerseIndex = -1;
			} else {
				activeVerseIndex = itemIndex;
			}
		}

		invalidate();
	}

	public void onDragComplete(final float x, final float y) {
		complete();

		removeCallbacks(checkLongPressBook);
		removeCallbacks(checkLongPressChapter);
		this.books = null; // prevent holding of memory
		this.state = State.idle;
	}

	void commitBook() {
		if (activeBookIndex == -1) {
			// do nothing, user hasn't selected a book
			return;
		}

		final Book book = books[activeBookIndex];
		if (book.chapter_count == 1) {
			listener.onSelectComplete(Ari.encode(book.bookId, 1, 1));
			hide();
		} else {
			state = State.selectChapter;
			invalidate();
		}
	}

	void commitChapter() {
		if (activeChapterIndex == -1) {
			// do nothing, user hasn't selected a chapter
			return;
		}

		state = State.selectVerse;
		invalidate();
	}

	private void hide() {
		setVisibility(GONE);
	}

	void complete() {
		if (state == State.selectBook) {
			if (activeBookIndex != -1) {
				final Book book = books[activeBookIndex];
				listener.onSelectComplete(Ari.encode(book.bookId, 1, 1));
			}
		}

		if (state == State.selectChapter) {
			if (activeBookIndex != -1 && activeChapterIndex != -1) {
				final Book book = books[activeBookIndex];
				listener.onSelectComplete(Ari.encode(book.bookId, activeChapterIndex + 1, 1));
			}
		}

		if (state == State.selectVerse) {
			if (activeBookIndex != -1 && activeChapterIndex != -1 && activeVerseIndex != -1) {
				final Book book = books[activeBookIndex];
				listener.onSelectComplete(Ari.encode(book.bookId, activeChapterIndex + 1, activeVerseIndex + 1));
			}
		}
	}

	public void setListener(final Listener listener) {
		this.listener = listener;
	}
}
