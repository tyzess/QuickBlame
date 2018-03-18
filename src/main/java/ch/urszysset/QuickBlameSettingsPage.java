package ch.urszysset;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class QuickBlameSettingsPage implements Configurable {

    private DefaultListModel<QuickBlameMappingEntry> quickBlameMappingEntriesListModel = new DefaultListModel<>();
    private JBList<QuickBlameMappingEntry> quickBlameMappingEntriesList;
    private Map<String, String> quickBlameMap = new HashMap<>();

    public QuickBlameSettingsPage() {
        QuickBlameSettings instance = QuickBlameSettings.getInstance();
        setData(instance);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "QuickBlame";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        addEntriesToListModel(quickBlameMappingEntriesListModel, quickBlameMap);
        quickBlameMappingEntriesList = new JBList<>(quickBlameMappingEntriesListModel);
        quickBlameMappingEntriesList.setEmptyText("No QuickBlame author name mappings configured!");

        return ToolbarDecorator.createDecorator(quickBlameMappingEntriesList)
                .setAddAction(getAddActionButtonRunnable(quickBlameMap))
                .setRemoveAction(getRemoveActionButtonRunnable(quickBlameMap))
                .setEditAction(getEditActionButtonRunnable(quickBlameMap))
                .disableUpDownActions()
                .createPanel();
    }

    private void addEntriesToListModel(DefaultListModel<QuickBlameMappingEntry> listModel, Map<String, String> quickBlameMap) {
        quickBlameMap.forEach((key, value) -> listModel.addElement(new QuickBlameMappingEntry(key, value)));
    }

    private AnActionButtonRunnable getEditActionButtonRunnable(Map<String, String> entries) {
        return anActionButton -> {
            QuickBlameMappingEntry oldQuickBlameMappingEntry = quickBlameMappingEntriesList.getSelectedValue();
            QuickBlameMappingEntry entry = showFastBlameEntryDialog(oldQuickBlameMappingEntry);
            if (entry != null && !entry.equals(oldQuickBlameMappingEntry)) {
                entries.remove(oldQuickBlameMappingEntry.getKey());
                entries.put(entry.getKey(), entry.getValue());
                quickBlameMappingEntriesListModel.removeElement(oldQuickBlameMappingEntry);
                quickBlameMappingEntriesListModel.addElement(entry);
            }
        };
    }

    @NotNull
    private AnActionButtonRunnable getRemoveActionButtonRunnable(Map<String, String> entries) {
        return anActionButton -> {
            for (QuickBlameMappingEntry selectedEntry : quickBlameMappingEntriesList.getSelectedValuesList()) {
                entries.remove(selectedEntry.getKey());
                quickBlameMappingEntriesListModel.removeElement(selectedEntry);
            }
        };
    }

    @NotNull
    private AnActionButtonRunnable getAddActionButtonRunnable(Map<String, String> entries) {
        return anActionButton -> {
            QuickBlameMappingEntry entry = showFastBlameEntryDialog();
            if (entry != null) {
                entries.put(entry.getKey(), entry.getValue());
                quickBlameMappingEntriesListModel.addElement(entry);
            }
        };
    }

    private QuickBlameMappingEntry showFastBlameEntryDialog() {
        return showFastBlameEntryDialog("", "");
    }

    private QuickBlameMappingEntry showFastBlameEntryDialog(QuickBlameMappingEntry quickBlameMappingEntry) {
        return showFastBlameEntryDialog(quickBlameMappingEntry.getKey(), quickBlameMappingEntry.getValue());
    }

    private QuickBlameMappingEntry showFastBlameEntryDialog(String key, String value) {
        FastBlameEntryDialog fastBlameEntryDialog = new FastBlameEntryDialog(key, value);
        fastBlameEntryDialog.show();
        return fastBlameEntryDialog.getFastBlameEntry();
    }

    private class FastBlameEntryDialog extends DialogWrapper {

        private final static String DIALOG_TITLE = "QuickBlame Mapping";
        private JPanel panel;
        private JTextField keyField;
        private JTextField valueField;

        FastBlameEntryDialog(String key, String value) {
            super(null);
            setTitle(DIALOG_TITLE);

            panel = new JPanel();

            keyField = new JTextField(20);
            keyField.setText(key);
            valueField = new JTextField(20);
            valueField.setText(value);

            panel.add(keyField);
            panel.add(new JLabel("->"));
            panel.add(valueField);

            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return panel;
        }

        QuickBlameMappingEntry getFastBlameEntry() {
            return new QuickBlameMappingEntry(keyField.getText(), valueField.getText());
        }

        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            if (keyField.getText().isEmpty()) {
                return new ValidationInfo("Key field cannot be empty!", keyField);
            }
            if (valueField.getText().isEmpty()) {
                return new ValidationInfo("Value field cannot be empty!", valueField);
            }
            if (containsKey()) {
                return new ValidationInfo("Key must be unique!", keyField);
            }
            return null;
        }

        private boolean containsKey() {
            return quickBlameMap.keySet().stream()
                    .anyMatch(quickBlameKey -> quickBlameKey.equals(keyField.getText()));
        }
    }

    @Override
    public boolean isModified() {
        return !QuickBlameSettings.getInstance().getQuickBlameMap().equals(quickBlameMap);
    }

    @Override
    public void apply() throws ConfigurationException {
        QuickBlameSettings instance = QuickBlameSettings.getInstance();
        getData(instance);
    }

    private void getData(QuickBlameSettings instance) {
        instance.setQuickBlameMap(quickBlameMap);
    }

    private void setData(QuickBlameSettings instance) {
        quickBlameMap = instance.getQuickBlameMap();
    }
}
