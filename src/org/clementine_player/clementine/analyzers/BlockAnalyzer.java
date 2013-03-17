package org.clementine_player.clementine.analyzers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.view.SurfaceHolder;

public class BlockAnalyzer extends BaseAnalyzer {
  public BlockAnalyzer(SurfaceHolder holder) {
    super(holder);
  }

  @Override
  protected void Update(byte[] fft, Canvas canvas) {
    int width = canvas.getWidth();
    int height = canvas.getHeight();
    int width_per_bar = width / (fft.length + 1);
    
    Paint bar_paint = new Paint();
    bar_paint.setColor(Color.rgb(255, 0, 0));
    
    Paint background_paint = new Paint();
    background_paint.setColor(Color.rgb(0, 0, 0));
    
    for (int i=0 ; i<fft.length ; ++i) {
      int bar_height = fft[i] * height / 255;
      
      canvas.drawRect(
          i * width_per_bar, 0,
          (i+1) * width_per_bar, bar_height,
          bar_paint);
      
      canvas.drawRect(
          i * width_per_bar, bar_height,
          (i+1) * width_per_bar, height,
          background_paint);
    }
  }
}
