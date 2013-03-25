package org.clementine_player.clementine.analyzers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

public class BlockAnalyzer extends BaseAnalyzer {
  private static final String TAG = "BlockAnalyzer";
  
  private static final int kBlockWidth = 4;
  private static final int kBlockHeight = 2;
  private static final int kBlockSpacing = 1;
  private static final int kFadeSize = 90;
  private static final int kFallingLineRate = 1;
  private static final int kFadeIntensityRate = 1;
  
  private int rows_;
  private int columns_;
  private int[] row_scale_;
  private int[] falling_line_;
  private int[] fade_pos_;
  private int[] fade_intensity_;
  private Bitmap bar_bitmap_;
  private Bitmap[] fade_bitmaps_;
  
  public BlockAnalyzer(SurfaceHolder holder) {
    super(holder);
  }
  
  @Override
  protected void SizeChanged(int width, int height) {
    Log.i(TAG, "Size changed to " + width + "x" + height);
    
    rows_ = (height + kBlockSpacing) / (kBlockHeight + kBlockSpacing);
    columns_ = (width + kBlockSpacing) / (kBlockWidth + kBlockSpacing);
    
    row_scale_ = new int[rows_ + 1];
    for (int i=0 ; i<rows_ ; ++i) {
      row_scale_[i] =
          (int) ((1.0 - Math.log10(i + 1) / Math.log10(rows_ + 1)) * 192);
      Log.i(TAG, "Row scale " + i + " is " + row_scale_[i]);
    }
    row_scale_[rows_] = 0;
    
    falling_line_ = new int[columns_];
    fade_pos_ = new int[columns_];
    fade_intensity_ = new int[columns_];
    
    for (int i=0 ; i<columns_ ; ++i) {
      falling_line_[i] = 0;
      fade_pos_[i] = 50;
      fade_intensity_[i] = 32;
    }
    
    bar_bitmap_ = CreateBarBitmap();
    fade_bitmaps_ = CreateFadeBitmaps();
  }
  
  private int BackgroundColour() {
    return Color.BLACK;
  }
  
  private int ForegroundColour() {
    return Color.RED;
  }
  
  private Bitmap CreateBarBitmap() {
    Bitmap ret = Bitmap.createBitmap(
        kBlockWidth,
        rows_ * (kBlockHeight + kBlockSpacing) - kBlockSpacing,
        Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(ret);
    
    Rect rect = new Rect();
    Paint paint = new Paint();
    
    // Fill the image with the background colour.
    paint.setColor(BackgroundColour());
    rect.set(0, 0, ret.getWidth(), ret.getHeight());
    canvas.drawRect(rect, paint);
    
    // Draw each block.
    int y = 0;
    for (int i=0 ; i<rows_ ; ++i) {
      int value = 128 +
          (int) ((1.0 - Math.log10(i + 1) / Math.log10(rows_ + 2)) * 127);
      paint.setARGB(255, value, 0, 0);
      rect.set(0, y, kBlockWidth, y + kBlockHeight);
      canvas.drawRect(rect, paint);
      
      y += kBlockHeight + kBlockSpacing;
    }
    
    return ret;
  }
  
  private Bitmap[] CreateFadeBitmaps() {
    Rect rect = new Rect();
    Paint paint = new Paint();
    
    Bitmap[] ret = new Bitmap[kFadeSize];
    for (int i=0 ; i<kFadeSize ; ++i) {
      Bitmap bitmap = Bitmap.createBitmap(
          kBlockWidth,
          rows_ * (kBlockHeight + kBlockSpacing) - kBlockSpacing,
          Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      
      // Fill the image with the background colour.
      paint.setColor(BackgroundColour());
      rect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
      canvas.drawRect(rect, paint);
      
      double value = 1.0 - Math.log10(kFadeSize - i) / Math.log10(kFadeSize);
      paint.setARGB(255, (int) (value * 128), 0, 0);
      Log.i(TAG, "Creating fade " + i + " = " + value);
      
      // Draw each block
      for (int y=0 ; y<rows_ ; ++y) {
        rect.set(
            0,
            y * (kBlockHeight + kBlockSpacing),
            kBlockWidth,
            y * (kBlockHeight + kBlockSpacing) + kBlockHeight);
        canvas.drawRect(rect, paint);
      }
      
      ret[i] = bitmap;
    }
    return ret;
  }

  @Override
  protected void Update(int[] fft, Canvas canvas) {
    final int height = canvas.getHeight();
    
    Rect source_rect = new Rect();
    Rect dest_rect = new Rect();
    
    int pixel_x = 0;
    
    Paint background = new Paint();
    background.setColor(Color.BLACK);
    canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), background);
    
    for (int x=0 ; x<columns_ ; ++x) {
      // TODO(dsansome): interpolate instead.
      if (x >= fft.length) {
        break;
      }
      
      int y = 0;
      while (fft[x] < row_scale_[y]) {
        y ++;
      }
      
      if (y > falling_line_[x]) {
        y = falling_line_[x] + kFallingLineRate;
      }
      falling_line_[x] = y;
      
      if (y <= fade_pos_[x]) {
        fade_pos_[x] = y;
        fade_intensity_[x] = kFadeSize;
      }
      
      if (fade_intensity_[x] > 0) {
        int fade_index = fade_intensity_[x] -= kFadeIntensityRate;
        int fade_pixel_y = fade_pos_[x] * (kBlockHeight + kBlockSpacing);
        
        source_rect.set(0, 0, kBlockWidth, height - fade_pixel_y);
        dest_rect.set(pixel_x,
                      fade_pixel_y,
                      pixel_x + kBlockWidth,
                      height);
        canvas.drawBitmap(
            fade_bitmaps_[fade_index], source_rect, dest_rect, null);
      }
      
      if (fade_intensity_[x] == 0) {
        fade_pos_[x] = rows_;
      }
    
      int pixel_y = y * (kBlockHeight + kBlockSpacing);
      source_rect.set(0, pixel_y, kBlockWidth, height);
      dest_rect.set(pixel_x, pixel_y, pixel_x + kBlockWidth, height);
      canvas.drawBitmap(bar_bitmap_, source_rect, dest_rect, null);
      
      pixel_x += kBlockWidth + kBlockSpacing;
    }
  }
}
