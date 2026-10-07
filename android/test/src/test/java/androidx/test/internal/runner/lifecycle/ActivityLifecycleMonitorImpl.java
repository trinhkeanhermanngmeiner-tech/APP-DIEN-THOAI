package androidx.test.internal.runner.lifecycle;
import androidx.test.runner.lifecycle.*; import android.app.Activity; import java.util.*;
public class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
  private final Map<Activity, Stage> st = new LinkedHashMap<>(); private final List<ActivityLifecycleCallback> cb = new ArrayList<>();
  public void signalLifecycleChange(Stage s, Activity a){ st.put(a, s); for (ActivityLifecycleCallback c : new ArrayList<>(cb)) c.onActivityLifecycleChanged(a, s); }
  public void addLifecycleCallback(ActivityLifecycleCallback c){ cb.add(c); } public void removeLifecycleCallback(ActivityLifecycleCallback c){ cb.remove(c); }
  public Collection<Activity> getActivitiesInStage(Stage s){ List<Activity> r = new ArrayList<>(); for (Map.Entry<Activity, Stage> e : st.entrySet()) if (e.getValue() == s) r.add(e.getKey()); return r; }
  public Stage getLifecycleStageOf(Activity a){ return st.get(a); } }
