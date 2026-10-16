package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.groundstation.ContactWindow;

public interface UplinkChannel {

    UplinkResult transmit(Telecommand command, ContactWindow window);

    record UplinkResult(boolean transmitted, String detail) {

        public static UplinkResult transmitted(String detail) {
            return new UplinkResult(true, detail);
        }

        public static UplinkResult failed(String detail) {
            return new UplinkResult(false, detail);
        }
    }
}
