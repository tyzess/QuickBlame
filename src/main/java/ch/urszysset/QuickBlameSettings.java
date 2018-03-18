package ch.urszysset;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@State(name = "QuickBlameSettings", storages = {@Storage(file = "quickBlame.xml")})
public class QuickBlameSettings implements PersistentStateComponent<QuickBlameSettings> {

    private Map<String, String> blameMap;

    public Map<String, String> getBlameMap() {
        return blameMap;
    }

    public void setBlameMap(Map<String, String> blameMap) {
        this.blameMap = blameMap;
    }

    public static QuickBlameSettings getInstance() {
        return ServiceManager.getService(QuickBlameSettings.class);
    }

    @Nullable
    @Override
    public QuickBlameSettings getState() {
        return this;
    }

    @Override
    public void loadState(QuickBlameSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
