package org.clementine_player.clementine.analyzers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

public class BlockAnalyzer extends BaseAnalyzer {
  private static final String TAG = "BlockAnalyzer";
  
  private static final int kBaseBlockWidth = 4;
  private static final int kBaseBlockHeight = 2;
  private static final int kBaseBlockSpacing = 1;
  private static final int kFadeSize = 90;
  private static final int kFallingLineRate = 1;
  private static final int kFadeIntensityRate = 1;
  
  private int block_width_;
  private int block_height_;
  private int block_spacing_;
  
  private int rows_;
  private int columns_;
  private int[] row_scale_;
  private int[] falling_line_;
  private int[] fade_pos_;
  private int[] fade_intensity_;
  private Bitmap bar_bitmap_;
  private Bitmap[] fade_bitmaps_;
  
  public BlockAnalyzer(Context context, SurfaceHolder holder) {
    super(context, holder);
    
    final float density = screen_density();
    block_width_ = (int) (density * kBaseBlockWidth);
    block_height_ = (int) (density * kBaseBlockHeight);
    block_spacing_ = (int) (density * kBaseBlockSpacing);
  }
  
  @Override
  protected void SizeChanged(int width, int height) {
    Log.i(TAG, "Size changed to " + width + "x" + height);
    
    rows_ = (height + block_spacing_) / (block_height_ + block_spacing_);
    columns_ = (width + block_spacing_) / (block_width_ + block_spacing_);
    
    row_scale_ = new int[rows_ + 1];
    for (int i=0 ; i<rows_ ; ++i) {
      row_scale_[i] =
          (int) ((1.0 - Math.log10(i + 1) / Math.log10(rows_ + 1)) * 128);
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
  
  private float[] ForegroundColourHSV() {
    return new float[]{198f, 0.80f, 0.90f};
  }
  
  private Bitmap CreateBarBitmap() {
    Bitmap ret = Bitmap.createBitmap(
        block_width_,
        rows_ * (block_height_ + block_spacing_) - block_spacing_,
        Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(ret);
    
    Rect rect = new Rect();
    Paint paint = new Paint();
    
    // Fill the image with the background colour.
    paint.setColor(BackgroundColour());
    rect.set(0, 0, ret.getWidth(), ret.getHeight());
    canvas.drawRect(rect, paint);
    
    final float[] initial_hsv = ForegroundColourHSV();
    
    // Draw each block.
    int y = 0;
    for (int i=0 ; i<rows_ ; ++i) {
      float[] hsv = initial_hsv.clone();
      hsv[1] = (1.0f - (float)(i) / rows_) * initial_hsv[1];
      
      paint.setColor(Color.HSVToColor(hsv));
      rect.set(0, y, block_width_, y + block_height_);
      canvas.drawRect(rect, paint);
      
      y += block_height_ + block_spacing_;
    }
    
    return ret;
  }
  
  private Bitmap[] CreateFadeBitmaps() {
    Rect rect = new Rect();
    Paint paint = new Paint();
    
    final float[] initial_hsv = ForegroundColourHSV();
    
    Bitmap[] ret = new Bitmap[kFadeSize];
    for (int i=0 ; i<kFadeSize ; ++i) {
      Bitmap bitmap = Bitmap.createBitmap(
          block_width_,
          rows_ * (block_height_ + block_spacing_) - block_spacing_,
          Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      
      // Fill the image with the background colour.
      paint.setColor(BackgroundColour());
      rect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
      canvas.drawRect(rect, paint);
      
      float[] hsv = initial_hsv.clone();
      hsv[2] *= (float) ((1.0 - Math.log10(kFadeSize - i) / Math.log10(kFadeSize)));

      paint.setColor(Color.HSVToColor(hsv));
      
      // Draw each block
      for (int y=0 ; y<rows_ ; ++y) {
        rect.set(
            0,
            y * (block_height_ + block_spacing_),
            block_width_,
            y * (block_height_ + block_spacing_) + block_height_);
        canvas.drawRect(rect, paint);
      }
      
      ret[i] = bitmap;
    }
    return ret;
  }

  @Override
  protected void Update(int[] fft, Canvas canvas) {
    final int height = rows_ * (block_height_ + block_spacing_) - block_spacing_;
    
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
        int fade_pixel_y = fade_pos_[x] * (block_height_ + block_spacing_);
        
        source_rect.set(0, 0, block_width_, height - fade_pixel_y);
        dest_rect.set(pixel_x,
                      fade_pixel_y,
                      pixel_x + block_width_,
                      height);
        canvas.drawBitmap(
            fade_bitmaps_[fade_index], source_rect, dest_rect, null);
      }
      
      if (fade_intensity_[x] == 0) {
        fade_pos_[x] = rows_;
      }
    
      int pixel_y = y * (block_height_ + block_spacing_);
      source_rect.set(0, 0, block_width_, height - pixel_y);
      dest_rect.set(pixel_x, pixel_y, pixel_x + block_width_, height);
      canvas.drawBitmap(bar_bitmap_, source_rect, dest_rect, null);
      
      pixel_x += block_width_ + block_spacing_;
    }
  }
}
