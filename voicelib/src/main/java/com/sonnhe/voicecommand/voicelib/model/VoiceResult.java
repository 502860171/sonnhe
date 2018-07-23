package com.sonnhe.voicecommand.voicelib.model;

import java.util.List;

/**
 * 语音回传结果
 */
public class VoiceResult {
    private int code;
    private String message;
    private String dataText;
    private String dataSemantic;
    private String dataFrom;
    private List<SemanticResult> mSemanticResults;
    private String semanticTts;

    public VoiceResult() {
        super();
    }

    public String getSemanticTts() {
        return semanticTts;
    }

    public void setSemanticTts(String semanticTts) {
        this.semanticTts = semanticTts;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataText() {
        return dataText;
    }

    public void setDataText(String dataText) {
        this.dataText = dataText;
    }

    public String getDataSemantic() {
        return dataSemantic;
    }

    public void setDataSemantic(String dataSemantic) {
        this.dataSemantic = dataSemantic;
    }

    public String getDataFrom() {
        return dataFrom;
    }

    public void setDataFrom(String dataFrom) {
        this.dataFrom = dataFrom;
    }

    public List<SemanticResult> getSemanticResults() {
        return mSemanticResults;
    }

    public void setSemanticResults(List<SemanticResult> semanticResults) {
        mSemanticResults = semanticResults;
    }
}
