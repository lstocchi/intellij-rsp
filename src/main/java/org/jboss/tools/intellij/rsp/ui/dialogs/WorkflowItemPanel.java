package org.jboss.tools.intellij.rsp.ui.dialogs;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.Attribute;
import org.jboss.tools.rsp.api.dao.WorkflowPromptDetails;
import org.jboss.tools.rsp.api.dao.WorkflowResponseItem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class WorkflowItemPanel extends JPanel implements DocumentListener, ActionListener {
    private static final String COMBO_TRUE = "Yes (true)";
    private static final String COMBO_FALSE = "No (false)";

    private final WorkflowResponseItem item;
    private Map<String, Object> values;
    private ComboBox box;
    private JTextField field;

    public WorkflowItemPanel(WorkflowResponseItem item, Map<String, Object> values) {
        this.item = item;
        this.values = values;
        String type = item.getItemType();
        String content = item.getContent();
        String msg = item.getLabel() + (content == null || content.isEmpty() ? "" : "\n" + content);

        if( type == null || "workflow.prompt.small".equals(type)) {
            handleSmall(item, msg);
        } else if( "workflow.prompt.large".equals(type)) {
            handleLarge(item, msg);
        }

    }

    private void handleSmall(WorkflowResponseItem item, String msg) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        if (msg != null && !msg.isEmpty()) {
            add(new JLabel(msg));
        }
        handleInput(item, values);
    }

    private void handleLarge(WorkflowResponseItem item, String msg) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        if (msg != null && !msg.isEmpty()) {
            add(createTextArea(msg));
        }
        handleInput(item, values);
    }

    private JBScrollPane createTextArea(String msg) {
        JTextArea jta = new JTextArea(40,70);
        jta.setEditable(false);
        if( msg != null )
            jta.setText(msg);
        jta.setLineWrap(true);

        JBScrollPane scroll = new JBScrollPane(jta);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private void handleInput(WorkflowResponseItem item, Map<String, Object> values) {
        WorkflowPromptDetails details = item.getPrompt();
        if( details != null ) {
            if( details.getResponseType().equals(ServerManagementAPIConstants.ATTR_TYPE_BOOL)) {
                String[] vals = new String[]{COMBO_TRUE, COMBO_FALSE };
                box = new ComboBox(vals);
                box.setSelectedIndex(-1);
                box.addActionListener(this);
                add(box);
            }  else if( details.getResponseType().equals(ServerManagementAPIConstants.ATTR_TYPE_INT)) {
                field = item.getPrompt().isResponseSecret() ? new JBPasswordField() : new JTextField();
                field.getDocument().addDocumentListener(this);
                add(field);
            }  else if( details.getResponseType().equals(ServerManagementAPIConstants.ATTR_TYPE_STRING)) {
                field = item.getPrompt().isResponseSecret() ? new JBPasswordField() : new JTextField();
                field.getDocument().addDocumentListener(this);
                add(field);
            }
        }
    }

    private String asString(String type, Object value) {
        if(ServerManagementAPIConstants.ATTR_TYPE_BOOL.equals(type)) {
            return value == null ? "false" : Boolean.toString("true".equalsIgnoreCase(value.toString()));
        }
        if(ServerManagementAPIConstants.ATTR_TYPE_INT.equals(type)) {
            if( value instanceof Number ) {
                return Integer.toString(((Number)value).intValue());
            } else {
                return Integer.toString(new Double(Double.parseDouble(value.toString())).intValue());
            }
        }
        return value.toString();
    }

    private Object asObject(String text) {
        String type = item.getPrompt().getResponseType();
        if(ServerManagementAPIConstants.ATTR_TYPE_BOOL.equals(type)) {
            if( COMBO_TRUE.equals(text))
                return Boolean.TRUE;
            if( COMBO_FALSE.equals(text))
                return Boolean.FALSE;
            return Boolean.parseBoolean(text);
        }
        if(ServerManagementAPIConstants.ATTR_TYPE_INT.equals(type)) {
            try {
                return Integer.parseInt(text);
            } catch(NumberFormatException nfe) {
                return null;
            }
        }
        return text;
    }


    @Override
    public void insertUpdate(DocumentEvent e) {
        values.put(item.getId(), asObject(getWidgetString()));
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        values.put(item.getId(), asObject(getWidgetString()));
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        values.put(item.getId(), asObject(getWidgetString()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        values.put(item.getId(), asObject(getWidgetString()));
    }

    private String getWidgetString() {
        return box == null ? field.getText() : box.getSelectedItem() == null ? null : box.getSelectedItem().toString();
    }
}
