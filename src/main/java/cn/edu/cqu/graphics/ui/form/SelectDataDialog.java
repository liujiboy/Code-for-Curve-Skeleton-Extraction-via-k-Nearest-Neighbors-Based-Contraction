package cn.edu.cqu.graphics.ui.form;

import cn.edu.cqu.graphics.Constants;
import cn.edu.cqu.graphics.platform.DataImporter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectDataDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox typeCombox;
    private JComboBox formatCombox;
    private JButton selectFileButton;
    private JTextField filePathText;
    private DataImporter listener = null;
    private Map<DataTypeItem, ArrayList<DataFormatItem>> map = new HashMap<>();
    private File selectedFile = null;

    public SelectDataDialog(JFrame father, DataImporter listener) {
        super(father);
        this.listener = listener;

        initData();
        initWindow();
        initComponent();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initData() {
        ArrayList<DataFormatItem> pointCloudFormats = new ArrayList<>();
        ArrayList<DataFormatItem> skeletonFormats = new ArrayList<>();
        pointCloudFormats.add(new DataFormatItem(Constants.FORMAT_POINT_CLOUD_LIUJI, "刘氏点云"));
        pointCloudFormats.add(new DataFormatItem(Constants.FORMAT_POINT_CLOUD_PLY, "ply格式"));
        pointCloudFormats.add(new DataFormatItem(Constants.FORMAT_POINT_CLOUD_ROSA_OFF, "off格式"));
        skeletonFormats.add(new DataFormatItem(Constants.FORMAT_SKELETON_ZJL, "简单骨架"));
        skeletonFormats.add(new DataFormatItem(Constants.FORMAT_SKELETON_L1_MEDIAN, "L1-median骨架"));
        map.put(new DataTypeItem(Constants.TYPE_POINT_CLOUD, "点云"), pointCloudFormats);
        map.put(new DataTypeItem(Constants.TYPE_COMMON_SKELETON_CURVE, "骨架"), skeletonFormats);
    }

    private void initComponent() {
        DefaultComboBoxModel<DataTypeItem> model = new DefaultComboBoxModel(map.keySet().toArray());
        typeCombox.setModel(model);
        formatCombox.setModel(new DefaultComboBoxModel(map.get(typeCombox.getSelectedItem()).toArray()));
        typeCombox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                DataTypeItem item = (DataTypeItem) typeCombox.getSelectedItem();
                formatCombox.setModel(new DefaultComboBoxModel(map.get(item).toArray()));
            }
        });

        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);
                chooser.addChoosableFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return true;
                    }

                    @Override
                    public String getDescription() {
                        return "*";
                    }
                });
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
//                    selectedFile = file;
                    filePathText.setText(file.getAbsolutePath());
                }

            }
        });
    }

    private void initWindow() {
        setMinimumSize(new Dimension(500, 300));
        setPreferredSize(new Dimension(500, 300));
        setTitle("导入数据");

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

    }

    private void onOK() {
        selectedFile = new File(filePathText.getText());
        if (! selectedFile.exists()) {
            System.err.println("所选文件不存在...");
            return;
        }
        // add your code here
        if (selectedFile != null && listener != null) {
            listener.loadData(selectedFile,
                    ((DataTypeItem)typeCombox.getSelectedItem()).dataType,
                    ((DataFormatItem)formatCombox.getSelectedItem()).dataFormat);
        }
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static class DataTypeItem {
        int dataType;
        String typeName;

        public DataTypeItem(int dataType, String typeName) {
            this.dataType = dataType;
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataTypeItem that = (DataTypeItem) o;

            if (dataType != that.dataType) return false;
            return typeName != null ? typeName.equals(that.typeName) : that.typeName == null;
        }

        @Override
        public int hashCode() {
            int result = dataType;
            result = 31 * result + (typeName != null ? typeName.hashCode() : 0);
            return result;
        }
    }

    public static class DataFormatItem {
        int dataFormat;
        String formatName;

        public DataFormatItem(int dataFormat, String formatName) {
            this.dataFormat = dataFormat;
            this.formatName = formatName;
        }

        @Override
        public String toString() {
            return formatName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataFormatItem that = (DataFormatItem) o;

            if (dataFormat != that.dataFormat) return false;
            return formatName != null ? formatName.equals(that.formatName) : that.formatName == null;
        }

        @Override
        public int hashCode() {
            int result = dataFormat;
            result = 31 * result + (formatName != null ? formatName.hashCode() : 0);
            return result;
        }
    }
}