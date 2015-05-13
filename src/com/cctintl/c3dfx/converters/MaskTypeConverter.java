/*
 * Copyright (c) 2015, CCT and/or its affiliates. All rights reserved.
 * CCT PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.cctintl.c3dfx.converters;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

import com.cctintl.c3dfx.controls.C3DRippler.RipplerMask;
import com.sun.javafx.css.StyleConverterImpl;

public final class MaskTypeConverter extends StyleConverterImpl<String , RipplerMask> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final MaskTypeConverter INSTANCE = new MaskTypeConverter();
    }
    public static StyleConverter<String, RipplerMask> getInstance() {
        return Holder.INSTANCE;
    }
    private MaskTypeConverter() {
        super();
    }

    @Override
    public RipplerMask convert(ParsedValue<String,RipplerMask> value, Font not_used) {
        String string = value.getValue();
        try {
            return RipplerMask.valueOf(string);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return RipplerMask.RECT;
        }
    }

    @Override
    public String toString() {
        return "MaskTypeConverter";
    }
}