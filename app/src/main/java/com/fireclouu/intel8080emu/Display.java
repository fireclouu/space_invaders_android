package com.fireclouu.intel8080emu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.fireclouu.intel8080emu.Emulator.BaseClass.DisplayAdapter;
import com.fireclouu.intel8080emu.Emulator.BaseClass.PlatformAdapter;
import com.fireclouu.intel8080emu.Emulator.BaseClass.StringUtils;
import com.fireclouu.intel8080emu.Emulator.Emulator;
import com.fireclouu.intel8080emu.Emulator.Interpreter;

import java.util.ArrayList;

public class Display extends SurfaceView implements SurfaceHolder.Callback, DisplayAdapter
{
	// get float value only
	// on emulation class devise array that can hold 0x2400 - 0x3fff and pass it here
	// do the loop here! instead of looping on another class
	
	// make tests for display
	Thread master;
	Canvas canvas;
	
	// make adaptive
	private final float GUEST_MACHINE_WIDTH = 224f;
	private final float GUEST_MACHINE_HEIGHT = 256f;
	private float PIXEL_SIZE = 3.18f;
	private float PIXEL_SIZE_WIDTH = PIXEL_SIZE;
	private float PIXEL_SIZE_HEIGHT = PIXEL_SIZE;

	Paint paintRed, paintWhite, paintGreen, paintText;
	
	private short[] memory;
	ArrayList<Float> plotList;
	SurfaceHolder holder;
	
	
	public Display(Context context) {
		super(context); 
		init();
	}
	
	public Display(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@Override
	public void surfaceCreated(SurfaceHolder p1) {
		// TODO
	}

	@Override
	public void surfaceChanged(SurfaceHolder p1, int p2, int p3, int p4) {
		// TODO: Implement this method
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder p1) {
		// TODO: Implement this method
	}
	private void init() {
		holder = getHolder();
		
		paintRed = setPaint(Color.RED);
		paintWhite = setPaint(Color.WHITE);
		paintGreen = setPaint(Color.GREEN);

		paintText = setPaint(Color.WHITE);
		paintText.setTextSize(12);
	}
	
	@Override
	public void updateView(short[] memory) {
		this.memory = memory;
	}
	
	@Override
	public float[] getPos(int orientation) {
		
		plotList = new ArrayList<>();

		short vram = INIT_VRAM;
		float[] plot;
		int counter = 0;
		
		int x = 0;
		int y = 0;
		int cond_x;
		int cond_y;
		
		boolean swap = false;
		int setBit;

        if (orientation == ORIENTATION_COUNTERCLOCK) {
            x = -DISPLAY_WIDTH;
            y = 0;
            setBit = -1;

            // swap
            swap = true;
        } else {
            x = 0;
            y = 0;
            setBit = 1;
        }
		
		// automate
		cond_x = (x == 0) ? DISPLAY_WIDTH  : 0;
		cond_y = (y == 0) ? DISPLAY_HEIGHT : 0;
		
		for (int ty = y; ty < cond_y; ty += 1)
		{
			for (int tx = x; tx < cond_x; tx += 8)
			{
				short data = this.memory[vram++];	// increment location of vram to decode
				for (int bit = 0; bit < 8; bit++)
				{
					if (( (data >> bit) & 1) == 1)
					{
						if (swap) {
							plotList.add((Math.abs(ty)) * PIXEL_SIZE_HEIGHT);
							plotList.add((Math.abs(tx) + (bit * setBit)) * PIXEL_SIZE_WIDTH);
						} else {
							plotList.add((Math.abs(tx) + (bit * setBit)) * PIXEL_SIZE_WIDTH);
							plotList.add((Math.abs(ty)) * PIXEL_SIZE_HEIGHT);
						}
					}
				}
			}	
		}
		
		plot = new float[plotList.size()];
		for (float pos : plotList) plot[counter++] = pos;
		return plot;
	}
	
	@Override
	public void startDisplay() {
		if(StringUtils.Component.DEBUG) {
			master = new Thread(new DebugThread());
		} else {
			master = new Thread(new DrawThread());
		}
		
		master.start();
	}
	
	private Paint setPaint(int color) {
		Paint mPaint;
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(color);
		
		return mPaint;
	}
	
	private double fps() {
		return 0;
	}
	private String parseFps(double fps) {
		return String.format("fps: %.2f", fps);
	}
	
	private float getAdaptiveSize() {
		float width = getWidth();
		float height = getHeight();
		Log.i(StringUtils.TAG, "WIDTH: " + width + " HEIGHT: " + height);
		// preserve aspect ration by not calculating height
		return width / GUEST_MACHINE_WIDTH;
	}
	
	class DrawThread implements Runnable {
	@Override
	public void run() {
		while (!holder.getSurface().isValid()) {
			// container
			PIXEL_SIZE = getAdaptiveSize();
			PIXEL_SIZE_WIDTH = PIXEL_SIZE;
			PIXEL_SIZE_HEIGHT = PIXEL_SIZE;
			paintWhite.setStrokeWidth(PIXEL_SIZE);
		}
		
		while (memory == null) continue;
		while(Emulator.stateMaster) {
			if (!holder.getSurface().isValid() & !holder.isCreating()) continue;
			
			// canvas
			canvas = holder.lockCanvas();
			canvas.drawColor(Color.BLACK);
			canvas.drawPoints(getPos(ORIENTATION_COUNTERCLOCK), paintWhite);
			canvas.drawText(Platform.OUT_MSG, 0, 10, paintWhite);
			
			// fps
			canvas.drawText(parseFps(fps()), 0, getHeight() - 10, paintWhite);
			// Emulation thread speed
			canvas.drawText("Thread speed: " + Emulator.actual_cycle, 0, getHeight() - 25, paintWhite);
			//cycle
			if (Emulator.isCycleCorrect()) {
				canvas.drawText(Emulator.cycleInfo, 0, getHeight() - 40, paintGreen);
			} else {
				canvas.drawText(Emulator.cycleInfo, 0, getHeight() - 40, paintRed);
			}
			
			canvas.drawText("fireclouu", (int) (getWidth() / 1.1), getHeight() - 10, paintWhite);
			
			// release
			holder.unlockCanvasAndPost(canvas);
		}
	}
	
}
class DebugThread implements Runnable {
		@Override
		public void run() {
			while(Emulator.stateMaster) {
				if (!holder.getSurface().isValid()) continue;
				
				canvas = holder.lockCanvas();
				
				canvas.drawColor(Color.BLACK);
				canvas.drawText(
					Platform.OUT_MSG, 0,
					10,
						paintWhite);
				canvas.drawText(
					StringUtils.getTime(), getWidth() - 60, 15, paintWhite);
				
				long expected = 23803381171L; // 24 billion
				try {
				int startingpoint = 20;
				for (String msg : PlatformAdapter.BUILD_MSG) {
					canvas.drawText(msg, 0, startingpoint += 20, paintText);
				} 
					
				}catch (NullPointerException e) {
					String exception = e.getMessage() != null ? e.getMessage() : "DebugThread: Message is null";
					Log.e(StringUtils.TAG, exception);
				}
				
				
				canvas.drawText(
					"Hardware accelerated: " + isHardwareAccelerated(), 0, 
					getHeight() - 10,
						paintWhite);

				canvas.drawText(
					"Expected Cpu Cycle: " + expected, 0, 
					getHeight() - 25,
						paintWhite);
					
				canvas.drawText(
					"Remaining Cpu Cycle: " + (expected - Interpreter.cycle), 0, 
					getHeight() - 40,
						paintWhite);
					
				canvas.drawText(
					"Current Cpu Cycle: " + Interpreter.cycle, 0, 
					getHeight() - 55,
						paintWhite);
					
				canvas.drawText(
					"fireclouu", (int) (getWidth() / 1.1), 
					getHeight() - 10,
						paintWhite);
				
				holder.unlockCanvasAndPost(canvas);
			}
		}
}
}