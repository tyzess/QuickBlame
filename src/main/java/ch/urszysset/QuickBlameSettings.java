package ch.urszysset;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(name = "QuickBlameSettings", storages = {@Storage(file = "quickBlame.xml")})
public class QuickBlameSettings implements PersistentStateComponent<QuickBlameSettings> {

    private Map<String, String> quickBlameMap = new HashMap<>();

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

    public Map<String, String> getQuickBlameMap() {
        return quickBlameMap;
    }

    public void setQuickBlameMap(Map<String, String> quickBlameMap) {
        this.quickBlameMap = quickBlameMap;
    }

    public boolean containsMappingForAuthor(String author) {
        return quickBlameMap.keySet().stream()
                .anyMatch(key -> key.equals(author));
    }

    public String getMappedAuthorName(String author) {
        return quickBlameMap.get(author);
    }
}
