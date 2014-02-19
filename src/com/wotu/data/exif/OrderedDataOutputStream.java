package com.wotu.data.exif;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class OrderedDataOutputStream extends FilterOutputStream {
    private final ByteBuffer mByteBuffer = ByteBuffer.allocate(4);

    public OrderedDataOutputStream(OutputStream out) {
        super(out);
    }

    public OrderedDataOutputStream setByteOrder(ByteOrder order) {
        mByteBuffer.order(order);
        return this;
    }

    public OrderedDataOutputStream writeShort(short value) throws IOException {
        mByteBuffer.rewind();
        mByteBuffer.putShort(value);
        out.write(mByteBuffer.array(), 0, 2);
        return this;
    }

    public OrderedDataOutputStream writeInt(int value) throws IOException {
        mByteBuffer.rewind();
        mByteBuffer.putInt(value);
        out.write(mByteBuffer.array());
        return this;
    }

    public OrderedDataOutputStream writeRational(Rational rational) throws IOException {
        writeInt((int) rational.getNumerator());
        writeInt((int) rational.getDenominator());
        return this;
    }
}
