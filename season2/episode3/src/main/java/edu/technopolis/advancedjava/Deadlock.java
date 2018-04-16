package edu.technopolis.advancedjava;

import com.sun.xml.internal.fastinfoset.tools.FI_SAX_Or_XML_SAX_DOM_SAX_SAXEvent;

public class Deadlock {
    private static final Object FIRST_LOCK = new Object();
    private static final Object SECOND_LOCK = new Object();

    public static void main(String[] args) throws Exception {
        Thread ft = new Thread(Deadlock::first);
        Thread st = new Thread(Deadlock::second);
        ft.start();
        st.start();
        ft.join();
        st.join();
        //never going to reach this point
    }

    private static void first() {
        synchronized(FIRST_LOCK) {
            //insert some code here to guarantee a deadlock
            synchronized(SECOND_LOCK) {
                try {
                    SECOND_LOCK.wait();
                } catch (InterruptedException e) {}
                //unreachable point
            }
        }
    }

    private static void second() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //
        }
        //reverse order of monitors
        synchronized(SECOND_LOCK) {
            SECOND_LOCK.notifyAll();
            //insert some code here to guarantee a deadlock
            synchronized(FIRST_LOCK) {
                //unreachable point
            }
        }

    }

}
