package com.wotu.anim;

import com.wotu.view.opengl.GLCanvas;


public abstract class CanvasAnimation extends Animation {

    public abstract int getCanvasSaveFlags();
    public abstract void apply(GLCanvas canvas);
}
