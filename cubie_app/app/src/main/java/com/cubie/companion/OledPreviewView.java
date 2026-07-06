package com.cubie.companion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class OledPreviewView extends View {
    private static byte[] frameData;

    private final Paint paint = new Paint();
    private final Bitmap bitmap = Bitmap.createBitmap(FacePackData.WIDTH, FacePackData.HEIGHT, Bitmap.Config.ARGB_8888);
    private final int[] pixels = new int[FacePackData.WIDTH * FacePackData.HEIGHT];
    private final Rect source = new Rect(0, 0, FacePackData.WIDTH, FacePackData.HEIGHT);
    private final Rect target = new Rect();

    private int facePackIndex = 0;
    private int frameIndex = 0;
    private long lastFrameAt = 0;

    OledPreviewView(Context context) {
        super(context);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        setBackgroundColor(Color.rgb(1, 24, 28));
        loadFrames(context);
    }

    void setFacePack(int index) {
        int safeIndex = Math.max(0, Math.min(index, FacePackData.NAMES.length - 1));
        if (facePackIndex != safeIndex) {
            facePackIndex = safeIndex;
            frameIndex = 0;
            lastFrameAt = 0;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        advanceFrame();
        drawCurrentFrame(canvas);
        postInvalidateDelayed(Math.max(16, FacePackData.FRAME_DELAYS_MS[facePackIndex]));
    }

    private void advanceFrame() {
        long now = System.currentTimeMillis();
        int delay = FacePackData.FRAME_DELAYS_MS[facePackIndex];
        if (lastFrameAt == 0) {
            lastFrameAt = now;
            return;
        }
        if (now - lastFrameAt >= delay) {
            int steps = (int) ((now - lastFrameAt) / delay);
            frameIndex = (frameIndex + Math.max(1, steps)) % FacePackData.FRAME_COUNTS[facePackIndex];
            lastFrameAt = now;
        }
    }

    private void drawCurrentFrame(Canvas canvas) {
        if (frameData == null) {
            canvas.drawColor(Color.rgb(1, 24, 28));
            return;
        }

        int offset = FacePackData.OFFSETS[facePackIndex] + frameIndex * FacePackData.FRAME_BYTES;
        if (offset < 0 || offset + FacePackData.FRAME_BYTES > frameData.length) {
            canvas.drawColor(Color.rgb(1, 24, 28));
            return;
        }

        int pixel = 0;
        for (int y = 0; y < FacePackData.HEIGHT; y++) {
            int row = offset + y * 16;
            for (int xByte = 0; xByte < 16; xByte++) {
                int value = frameData[row + xByte] & 0xFF;
                for (int bit = 0; bit < 8; bit++) {
                    boolean on = (value & (1 << (7 - bit))) != 0;
                    pixels[pixel++] = on ? Color.rgb(239, 246, 248) : Color.rgb(1, 24, 28);
                }
            }
        }

        bitmap.setPixels(pixels, 0, FacePackData.WIDTH, 0, 0, FacePackData.WIDTH, FacePackData.HEIGHT);
        target.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(bitmap, source, target, paint);
    }

    private static void loadFrames(Context context) {
        if (frameData != null) {
            return;
        }
        try (InputStream in = context.getAssets().open(FacePackData.ASSET_NAME);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            frameData = out.toByteArray();
        } catch (IOException ignored) {
            frameData = null;
        }
    }
}
