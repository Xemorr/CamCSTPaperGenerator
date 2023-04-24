package me.xemor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private Map<Integer, List<String>> paperContents;
    private Map<String, List<String>> pastQuestions;

    public Map<Integer, List<String>> getPaperContents() {
        return paperContents;
    }

    public void setPaperContents(Map<Integer, List<String>> paperContents) {
        this.paperContents = paperContents;
    }

    public Map<String, List<String>> getPastQuestions() {
        return pastQuestions;
    }

    public void setPastQuestions(Map<String, List<String>> pastQuestions) {
        this.pastQuestions = pastQuestions;
    }
}
