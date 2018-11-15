package uryde.passenger.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * The class is used as a medium to pass custom
 * Created by GoParties on 21/01/16.
 */
public class PookieEventBus {

    private static PookieEventBus eventManager;
    private HashMap<String, ArrayList<PookieEventBusReceiver>> receivers;

    private PookieEventBus() {
        receivers = new HashMap<String, ArrayList<PookieEventBusReceiver>>();
    }

    public static PookieEventBus getInstance() {
        if (eventManager == null)
            eventManager = new PookieEventBus();
        return eventManager;
    }

    public PookieEventBus subscribe(PookieEventReceiver receiver, String... terms) {
        for (String term : terms) {
            PookieEventBusReceiver gpEventBusReceiver = eventManager.new PookieEventBusReceiver(term, receiver);
            ArrayList<PookieEventBusReceiver> list = eventManager.getTerm(term);
            for (int i = 0; i < list.size(); ++i)
                if (list.get(i).equals(receiver))
                    return eventManager;
            list.add(gpEventBusReceiver);
        }
        return eventManager;
    }
    
    public PookieEventBus unSubscribe(Object object) {
        if (object instanceof PookieEventReceiver)
            unSubscribe((PookieEventReceiver) object);
        return eventManager;
    }

    private PookieEventBus unSubscribe(PookieEventReceiver receiver) {
        Set<String> keys = eventManager.receivers.keySet();
        String[] array = keys.toArray(new String[keys.size()]);
        unSubscribe(receiver, array);
        return eventManager;
    }

    public PookieEventBus unSubscribe(PookieEventReceiver receiver, String... terms) {
        for (String term : terms) {
            ArrayList<PookieEventBusReceiver> list = eventManager.getTerm(term);
            list.remove(eventManager.new PookieEventBusReceiver(term, receiver));
            if (list.size() == 0) {
                eventManager.receivers.remove(term);
            }
        }
        return eventManager;
    }

    public PookieEventBus unSubscribeAll(String... terms) {
        for (String term : terms) {
            eventManager.receivers.remove(term);
        }
        return eventManager;
    }
//// TODO: 5/4/16 create publisher with multiple object
    public PookieEventBus publish(String term, Object object) {
        ArrayList<PookieEventBusReceiver> list = eventManager.getTerm(term);
        for (int i = 0; i < list.size(); ++i) {
            list.get(i).publish(object);
        }
        return eventManager;
    }

    private ArrayList<PookieEventBusReceiver> getTerm(String term) {
        ArrayList<PookieEventBusReceiver> list = receivers.get(term);
        if (list == null) {
            list = new ArrayList<PookieEventBusReceiver>();
            receivers.put(term, list);
        }
        return list;
    }

    public interface PookieEventReceiver {
        void onEvent(String term, Object object);
    }

    private class PookieEventBusReceiver {
        String term;
        PookieEventReceiver pEventReceiver;
        Handler handler;

        public PookieEventBusReceiver(String term, PookieEventReceiver gpEventReceiver) {
            this.term = term;
            this.pEventReceiver = gpEventReceiver;
            handler = new Handler(Looper.myLooper());
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof PookieEventBusReceiver && ((PookieEventBusReceiver) obj).pEventReceiver == pEventReceiver;
        }

        private boolean tryCallPush(Object object) {
            try {
                pEventReceiver.getClass().getMethod("onEvent", String.class, object.getClass())
                        .invoke(pEventReceiver, term, object);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public void publish(final Object object) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!tryCallPush(object))
                            pEventReceiver.onEvent(term, object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
