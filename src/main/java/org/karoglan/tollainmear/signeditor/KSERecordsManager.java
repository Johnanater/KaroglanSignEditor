package org.karoglan.tollainmear.signeditor;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.karoglan.tollainmear.signeditor.utils.ClipBoardContents;
import org.karoglan.tollainmear.signeditor.utils.KSEStack;
import org.karoglan.tollainmear.signeditor.utils.Translator;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class KSERecordsManager {
    private String operationLog = "Operation_Log";

    private static KSERecordsManager instance;
    private KaroglanSignEditor kse;
    private KSEStack kseStack;

    private Translator translator;
    private File recorderFile;
    private String pluginName;
    private CommentedConfigurationNode rootNode;
    private CommentedConfigurationNode operationLogNode;
    private CommentedConfigurationNode clipBoardNode;
    private ConfigurationLoader<CommentedConfigurationNode> recordLoader;

    private static Map<String, KSEStack> operationStack = new LinkedHashMap<>();
    private static Map<String, ClipBoardContents> copylist = new LinkedHashMap<>();

    KSERecordsManager(KaroglanSignEditor plugin) throws IOException {
        kse = plugin;
        instance = this;
        recorderFile = new File(plugin.getConfigPath().toString() + "/records.yml");
        recordLoader = HoconConfigurationLoader.builder().setFile(recorderFile).build();
        pluginName = KaroglanSignEditor.getPluginName();
        translator = kse.getTranslator();
    }

    public void init(KaroglanSignEditor kse) throws IOException {
        operationStack.clear();
        copylist.clear();

        rootNode = recordLoader.load();
        operationLogNode = rootNode.getNode(pluginName).getNode("Operation_Log");
        clipBoardNode = rootNode.getNode(pluginName).getNode("Clipboard");

        if (!recorderFile.exists()) {
            if (!recorderFile.createNewFile()) {
                translator.logWarn("CouldNotCreate");
            }
        }
        if (rootNode.getNode(pluginName).isVirtual()) {
            rootNode.getNode(pluginName).setComment(translator.getstring("rec.main"));
        }
        if (clipBoardNode.isVirtual()) {
            clipBoardNode.setComment(translator.getstring("rec.Clipboard"));
        }
        if (operationLogNode.isVirtual()) {
            operationLogNode.setComment(translator.getstring("rec.OperationLog"));
        }
        recordLoader.save(rootNode);
        loadOperationHistory();
        loadCopyList();
        save();
    }


    public void save() throws IOException {
        for (String locNode : operationStack.keySet()) {
            kseStack = operationStack.get(locNode);
            Text[][] stackArray = kseStack.getTextStack();
            operationLogNode.getNode(locNode).getNode("now").setValue(kseStack.getNow());
            operationLogNode.getNode(locNode).getNode("tail").setValue(kseStack.getTail());
            operationLogNode.getNode(locNode).getNode("head").setValue(kseStack.getHead());
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 4; j++) {
                    operationLogNode
                            .getNode(locNode)
                            .getNode("Stack[" + i + "]")
                            .getNode("Line[" + j + "]")
                            .setValue(TextSerializers.FORMATTING_CODE.serialize(stackArray[i][j] == null ? Text.of("") : stackArray[i][j]));
                }
            }
        }
        if (kse.getConfigNode().getNode(pluginName).getNode("ClipBoardCache").getBoolean()) {
            for (String playerName : copylist.keySet()) {
                Text[] clipArray = copylist.get(playerName).get();
                for (int i = 0; i < 4; i++) {
                    clipBoardNode
                            .getNode(playerName)
                            .getNode("Line[" + i + "]")
                            .setValue(TextSerializers.FORMATTING_CODE.serialize(clipArray[i] == null ? Text.of("") : clipArray[i]));
                }
            }
        }

        recordLoader.save(rootNode);
    }

    public static KSERecordsManager getInstance() {
        return instance;
    }

    public static Map<String, ClipBoardContents> getCopylist() {
        return copylist;
    }

    private void loadOperationHistory() {
        if (operationLogNode.hasMapChildren()) {
            Set<Object> opSet = operationLogNode.getChildrenMap().keySet();
            for (Object loc : opSet) {
                KSEStack kseStack = new KSEStack();
                Text[][] textStack = new Text[10][4];
                kseStack.setNow(operationLogNode.getNode(loc.toString()).getNode("now").getInt());
                kseStack.setTail(operationLogNode.getNode(loc.toString()).getNode("tail").getInt());
                kseStack.setHead(operationLogNode.getNode(loc.toString()).getNode("head").getInt());
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 4; j++) {
                        textStack[i][j] = TextSerializers.FORMATTING_CODE.deserialize(
                                operationLogNode
                                        .getNode(loc)
                                        .getNode("Stack[" + i + "]")
                                        .getNode("Line[" + j + "]")
                                        .getString());
                    }
                }
                kseStack.set(textStack);
                operationStack.put(loc.toString(), kseStack);
            }
        }
    }

    private void loadCopyList() {
        if (kse.getConfigNode().getNode(pluginName).getNode("ClipBoardCache").getBoolean()) {
            if (clipBoardNode.hasMapChildren()) {
                ClipBoardContents cbc = new ClipBoardContents();
                Text[] textArray = new Text[4];
                Set cbSet = clipBoardNode.getChildrenMap().keySet();
                for (Object name : cbSet) {
                    for (int i = 0; i < 4; i++) {
                        textArray[i] = TextSerializers.FORMATTING_CODE.deserialize(
                                clipBoardNode.
                                        getNode(name.toString())
                                        .getNode("Line[" + i + "]")
                                        .getString());
                    }
                    cbc.setTextArray(textArray);
                    copylist.put(name.toString(), cbc);
                }
            }
        }
    }

    public static Map<String, KSEStack> getOperationStack() {
        return operationStack;
    }
}
