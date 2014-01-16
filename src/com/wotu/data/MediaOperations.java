package com.wotu.data;

public class MediaOperations {
    public static final int SUPPORT_DELETE = 1 << 0;
    public static final int SUPPORT_ROTATE = 1 << 1;
    public static final int SUPPORT_SHARE = 1 << 2;
    public static final int SUPPORT_CROP = 1 << 3;
    public static final int SUPPORT_SHOW_ON_MAP = 1 << 4;
    public static final int SUPPORT_SETAS = 1 << 5;
    public static final int SUPPORT_FULL_IMAGE = 1 << 6;
    public static final int SUPPORT_CACHE = 1 << 7;
    public static final int SUPPORT_EDIT = 1 << 8;
    public static final int SUPPORT_INFO = 1 << 9;
    public static final int SUPPORT_IMPORT = 1 << 10;
    public static final int SUPPORT_ALL = 0xffffffff;
}
