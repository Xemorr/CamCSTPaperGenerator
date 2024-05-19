package me.xemor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class Main {

    public static List<PDDocument> opened = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        saveDefaultConfig();
        FileInputStream reader = new FileInputStream("config.yml");
        Yaml yaml = new Yaml(new Constructor(Config.class, new LoaderOptions()));
        Config config = yaml.load(reader);
        reader.close();
        List<String> questions = config.getPaperContents().get(Integer.valueOf(args[0]));
        Random random = new Random();
        PDDocument doc = new PDDocument();
        opened.add(doc);
        String lastQuestion = "";
        Map<String, List<Integer>> rngsMap = new HashMap<>();
        for (String question : questions) {
            List<QuestionURL> questionURLs = getPDFURLs(question, config);
            int rng = random.nextInt(Math.min(config.getNumberOfHistoricalQuestionsPerModule(), questionURLs.size()));
            if (lastQuestion.equals(question)) {
                List<Integer> rngs = rngsMap.computeIfAbsent(question, (s) -> new ArrayList<>());
                while (rngs.contains(rng)) {
                    rng = random.nextInt(questionURLs.size());
                }
            }
            else {
                lastQuestion = question;
            }
            rngsMap.computeIfAbsent(question, (s) -> new ArrayList<>()).add(rng);
            QuestionURL questionURL = questionURLs.get(rng);
            PDDocument result = getPDF(questionURL);
            for (PDPage page : result.getPages()) {
                addAnswerLink(doc, page, questionURL);
                doc.importPage(page);
            }
        }
        doc.save(new File("output.pdf"));
        for (PDDocument document : opened) {
            document.close();
        }
    }

    public static void addAnswerLink(PDDocument doc, PDPage page, QuestionURL questionURL) {
        questionURL.answer.ifPresent((s) -> {
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                PDAnnotationLink link = new PDAnnotationLink();
                PDActionURI action = new PDActionURI();
                action.setURI(s);
                link.setAction(action);
                PDRectangle position = new PDRectangle();
                position.setLowerLeftX(10);
                position.setLowerLeftY(10);
                position.setUpperRightX(150);
                position.setUpperRightY(25);
                link.setRectangle(position);
                page.getAnnotations().add(link);

                cs.beginText();
                cs.newLineAtOffset(12, 12);
                cs.setFont(PDType1Font.COURIER_BOLD, 10);
                cs.showText("Click here for answers");
                cs.endText();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static List<QuestionURL> getPDFURLs(String topic, Config config) {
        List<QuestionURL> urls = new ArrayList<>();
        List<String> questionPages = config.getPastQuestions().get(topic);
        for (String url : questionPages) {
            try {
                Document document = Jsoup.connect(url).get();
                List<Node> nodes = document.select("#content > div:nth-child(1) > ul:nth-child(4)").get(0).childNodes();
                Iterator<Node> nodeIterator = nodes.iterator();
                nodeIterator.next(); // skip bogus textnode
                for (Iterator<Node> it = nodeIterator; it.hasNext(); ) {
                    Node node = it.next();
                    String question = node.firstChild().attr("href");
                    if (!question.contains("http")) {
                        question = "https://www.cl.cam.ac.uk/teaching/exams/pastpapers/" + question;
                    }
                    if (node.childNodeSize() > 2) {
                        String answer = node.childNode(2).attr("href");
                        urls.add(new QuestionURL(question, Optional.of(answer)));
                    }
                    else {
                        urls.add(new QuestionURL(question, Optional.empty()));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return urls;
    }

    public static PDDocument getPDF(QuestionURL url) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url.question).openStream())) {
            PDDocument question = PDDocument.load(in);
            opened.add(question);
            return question;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveDefaultConfig() throws IOException {
        if (new File("config.yml").exists()) return;
        BufferedInputStream stream  = new BufferedInputStream(Main.class.getResourceAsStream("/config.yml"));
        byte[] bytes = stream.readAllBytes();
        stream.close();
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter("config.yml"));
        for (byte b : bytes) {
            fileWriter.write(b);
        }
        fileWriter.close();
    }

    public record QuestionURL(String question, Optional<String> answer) {}
}