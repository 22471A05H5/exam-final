package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIQuestionService {
    
    @Value("${ai.provider:google}")
    private String aiProvider;
    
    @Value("${ai.google.api-key}")
    private String googleApiKey;
    
    @Value("${ai.google.model:gemini-1.5-flash}")
    private String googleModel;
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;
    
    @Value("${openai.model}")
    private String openaiModel;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<Map<String, String>> generateQuestions(String topic, String difficulty, int numberOfQuestions) {
        try {
            System.out.println("DEBUG: Generating questions for topic: " + topic + ", difficulty: " + difficulty + ", count: " + numberOfQuestions);
            System.out.println("DEBUG: AI Provider: " + aiProvider);
            System.out.println("DEBUG: Google API Key: " + (googleApiKey != null ? "Present" : "Missing"));
            
            if ("google".equalsIgnoreCase(aiProvider)) {
                return generateQuestionsWithGoogle(topic, difficulty, numberOfQuestions);
            } else if ("gemini".equalsIgnoreCase(aiProvider)) {
                return generateQuestionsWithGemini(topic, difficulty, numberOfQuestions);
            } else {
                return generateQuestionsWithOpenAI(topic, difficulty, numberOfQuestions);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate AI questions: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate AI questions: " + e.getMessage(), e);
        }
    }
    
    private List<Map<String, String>> generateQuestionsWithOpenAI(String topic, String difficulty, int numberOfQuestions) throws Exception {
        String prompt = createPrompt(topic, difficulty, numberOfQuestions);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiModel);
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(openaiApiUrl, HttpMethod.POST, entity, String.class);
        
        return parseAIResponse(response.getBody());
    }
    
    private List<Map<String, String>> generateQuestionsWithGoogle(String topic, String difficulty, int numberOfQuestions) throws Exception {
        System.out.println("DEBUG: Starting Google Gemini API call");
        
        // Validate inputs
        if (topic == null || topic.trim().isEmpty()) {
            throw new Exception("Topic cannot be null or empty");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new Exception("Difficulty cannot be null or empty");
        }
        if (numberOfQuestions <= 0) {
            throw new Exception("Number of questions must be positive");
        }
        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            throw new Exception("Google API key is not configured");
        }
        
        String prompt = createPrompt(topic, difficulty, numberOfQuestions);
        System.out.println("DEBUG: Prompt created: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);
        
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + googleModel + ":generateContent?key=" + googleApiKey;
        System.out.println("DEBUG: Making request to Google Gemini API");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("DEBUG: Response status: " + response.getStatusCode());
            
            if (response.getBody() == null) {
                throw new Exception("Received null response from Google Gemini API");
            }
            
            System.out.println("DEBUG: Response body: " + response.getBody());
            
            return parseGeminiResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("ERROR: Google Gemini API call failed: " + e.getMessage());
            e.printStackTrace();
            
            // Return fallback questions instead of throwing exception
            return createFallbackQuestions(topic, difficulty, numberOfQuestions);
        }
    }
    
    private List<Map<String, String>> generateQuestionsWithGemini(String topic, String difficulty, int numberOfQuestions) throws Exception {
        System.out.println("DEBUG: Starting Gemini API call");
        
        // Validate inputs
        if (topic == null || topic.trim().isEmpty()) {
            throw new Exception("Topic cannot be null or empty");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new Exception("Difficulty cannot be null or empty");
        }
        if (numberOfQuestions <= 0) {
            throw new Exception("Number of questions must be positive");
        }
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new Exception("Gemini API key is not configured");
        }
        if (geminiApiUrl == null || geminiApiUrl.trim().isEmpty()) {
            throw new Exception("Gemini API URL is not configured");
        }
        
        String prompt = createPrompt(topic, difficulty, numberOfQuestions);
        System.out.println("DEBUG: Prompt created: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);
        
        String url = geminiApiUrl + "?key=" + geminiApiKey;
        System.out.println("DEBUG: Making request to: " + geminiApiUrl);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("DEBUG: Response status: " + response.getStatusCode());
            
            if (response.getBody() == null) {
                throw new Exception("Received null response from Gemini API");
            }
            
            System.out.println("DEBUG: Response body: " + response.getBody());
            
            return parseGeminiResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("ERROR: Gemini API call failed: " + e.getMessage());
            e.printStackTrace();
            
            // Return fallback questions instead of throwing exception
            return createFallbackQuestions(topic, difficulty, numberOfQuestions);
        }
    }
    
    private String createPrompt(String topic, String difficulty, int numberOfQuestions) {
        return String.format(
            "Generate exactly %d multiple choice questions about %s with %s difficulty level. " +
            "Return ONLY a valid JSON array with no markdown formatting or extra text. " +
            "Each question must have these exact fields: " +
            "questionText, optionA, optionB, optionC, optionD, correctAnswer, explanation. " +
            "The correctAnswer field must be exactly 'A', 'B', 'C', or 'D'. " +
            "Make questions educational and relevant to %s concepts. " +
            "DO NOT include any HTML tags in the questions or options - use plain text only. " +
            "Example format: [{\"questionText\":\"What does HTML stand for?\",\"optionA\":\"HyperText Markup Language\",\"optionB\":\"High Tech Modern Language\",\"optionC\":\"Home Tool Markup Language\",\"optionD\":\"Hyperlink and Text Markup Language\",\"correctAnswer\":\"A\",\"explanation\":\"HTML stands for HyperText Markup Language\"}]",
            numberOfQuestions, topic, difficulty.toLowerCase(), topic
        );
    }
    
    private List<Map<String, String>> parseAIResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        
        // Clean the response to extract JSON
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();
        
        JsonNode questionsArray = objectMapper.readTree(content);
        List<Map<String, String>> questions = new ArrayList<>();
        
        for (JsonNode questionNode : questionsArray) {
            Map<String, String> question = new HashMap<>();
            
            // Validate that all required fields exist and are not empty
            String questionText = questionNode.path("questionText").asText("");
            String optionA = questionNode.path("optionA").asText("");
            String optionB = questionNode.path("optionB").asText("");
            String optionC = questionNode.path("optionC").asText("");
            String optionD = questionNode.path("optionD").asText("");
            String correctAnswer = questionNode.path("correctAnswer").asText("");
            String explanation = questionNode.path("explanation").asText("");
            
            // Skip questions with missing critical data
            if (questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || 
                optionC.isEmpty() || optionD.isEmpty() || correctAnswer.isEmpty()) {
                System.out.println("DEBUG: Skipping incomplete OpenAI question: " + questionText);
                continue;
            }
            
            question.put("questionText", questionText);
            question.put("optionA", optionA);
            question.put("optionB", optionB);
            question.put("optionC", optionC);
            question.put("optionD", optionD);
            question.put("correctAnswer", correctAnswer);
            question.put("explanation", explanation.isEmpty() ? "No explanation provided" : explanation);
            questions.add(question);
        }
        
        // If we don't have enough valid questions, throw exception to trigger fallback
        if (questions.isEmpty()) {
            throw new Exception("No valid questions found in OpenAI response");
        }
        
        return questions;
    }
    
    private List<Map<String, String>> parseGeminiResponse(String responseBody) throws Exception {
        System.out.println("DEBUG: Parsing Gemini response: " + responseBody);
        
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new Exception("Empty response from Gemini API");
        }
        
        JsonNode root = objectMapper.readTree(responseBody);
        
        // Check if response has error
        if (root.has("error")) {
            String errorMessage = root.path("error").path("message").asText();
            throw new Exception("Gemini API error: " + errorMessage);
        }
        
        // Check if candidates exist
        if (!root.has("candidates") || root.path("candidates").isEmpty()) {
            throw new Exception("No candidates in Gemini response");
        }
        
        JsonNode candidate = root.path("candidates").get(0);
        if (!candidate.has("content")) {
            throw new Exception("No content in Gemini candidate");
        }
        
        JsonNode content = candidate.path("content");
        if (!content.has("parts") || content.path("parts").isEmpty()) {
            throw new Exception("No parts in Gemini content");
        }
        
        String textContent = content.path("parts").get(0).path("text").asText();
        System.out.println("DEBUG: Extracted text content: " + textContent);
        
        // Clean the response to extract JSON
        textContent = textContent.trim();
        if (textContent.startsWith("```json")) {
            textContent = textContent.substring(7);
        }
        if (textContent.endsWith("```")) {
            textContent = textContent.substring(0, textContent.length() - 3);
        }
        textContent = textContent.trim();
        
        System.out.println("DEBUG: Cleaned JSON content: " + textContent);
        
        JsonNode questionsArray = objectMapper.readTree(textContent);
        List<Map<String, String>> questions = new ArrayList<>();
        
        for (JsonNode questionNode : questionsArray) {
            Map<String, String> question = new HashMap<>();
            
            // Validate that all required fields exist and are not empty
            String questionText = questionNode.path("questionText").asText("");
            String optionA = questionNode.path("optionA").asText("");
            String optionB = questionNode.path("optionB").asText("");
            String optionC = questionNode.path("optionC").asText("");
            String optionD = questionNode.path("optionD").asText("");
            String correctAnswer = questionNode.path("correctAnswer").asText("");
            String explanation = questionNode.path("explanation").asText("");
            
            // Skip questions with missing critical data
            if (questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || 
                optionC.isEmpty() || optionD.isEmpty() || correctAnswer.isEmpty()) {
                System.out.println("DEBUG: Skipping incomplete question: " + questionText);
                continue;
            }
            
            question.put("questionText", questionText);
            question.put("optionA", optionA);
            question.put("optionB", optionB);
            question.put("optionC", optionC);
            question.put("optionD", optionD);
            question.put("correctAnswer", correctAnswer);
            question.put("explanation", explanation.isEmpty() ? "No explanation provided" : explanation);
            questions.add(question);
        }
        
        System.out.println("DEBUG: Parsed " + questions.size() + " valid questions successfully");
        
        // If we don't have enough valid questions, throw exception to trigger fallback
        if (questions.isEmpty()) {
            throw new Exception("No valid questions found in AI response");
        }
        
        return questions;
    }
    
    private List<Map<String, String>> createFallbackQuestions(String topic, String difficulty, int numberOfQuestions) {
        System.out.println("DEBUG: Creating fallback questions for topic: " + topic);
        List<Map<String, String>> questions = new ArrayList<>();
        
        if (topic.toLowerCase().contains("html")) {
            String[][] htmlQuestions = {
                {"What does HTML stand for?", "HyperText Markup Language", "High Tech Modern Language", "Home Tool Markup Language", "Hyperlink and Text Markup Language", "A", "HTML stands for HyperText Markup Language, which is used to create web pages."},
                {"Which tag is used to create a hyperlink in HTML?", "<a>", "<link>", "<href>", "<url>", "A", "The <a> tag with href attribute is used to create hyperlinks in HTML."},
                {"What is the correct HTML tag for the largest heading?", "<h1>", "<heading>", "<head>", "<h6>", "A", "<h1> is the largest heading tag in HTML, with <h6> being the smallest."},
                {"Which HTML attribute specifies an alternate text for an image?", "alt", "title", "src", "href", "A", "The alt attribute provides alternative text for images when they cannot be displayed."},
                {"What is the correct HTML tag to make text bold?", "<b>", "<bold>", "<strong>", "Both A and C", "D", "Both <b> and <strong> tags can make text bold, though <strong> has semantic meaning."},
                {"Which HTML tag is used to create an unordered list?", "<ul>", "<ol>", "<list>", "<li>", "A", "The <ul> tag creates an unordered (bulleted) list in HTML."},
                {"What does the HTML <head> section contain?", "Metadata about the document", "Visible content", "Body text", "Images", "A", "The <head> section contains metadata like title, links to CSS, and other document information."},
                {"Which HTML tag is used to include CSS styles?", "<style>", "<css>", "<link>", "Both A and C", "D", "CSS can be included using <style> tags or <link> tags for external stylesheets."},
                {"Which tag pairs are used to create a table in HTML?", "<table>, <tr>, <td>", "<div>, <p>, <span>", "<header>, <footer>, <main>", "<article>, <aside>, <section>", "A", "Table tags include <table> for the table, <tr> for rows, and <td> for data cells."},
                {"What is the purpose of the <div> tag in HTML?", "To create a division or section", "To create a paragraph", "To create a heading", "To create a link", "A", "The <div> tag is used to create divisions or sections in HTML documents."},
                {"Which HTML tag is used to create a form?", "<form>", "<input>", "<field>", "<data>", "A", "The <form> tag is used to create HTML forms for user input."},
                {"What is the correct HTML tag for inserting a line break?", "<br>", "<break>", "<lb>", "<newline>", "A", "The <br> tag creates a line break in HTML."},
                {"Which attribute is used to specify the source of an image?", "src", "href", "link", "source", "A", "The src attribute specifies the source URL of an image in HTML."},
                {"What is the HTML tag for creating a paragraph?", "<p>", "<para>", "<paragraph>", "<text>", "A", "The <p> tag is used to create paragraphs in HTML."},
                {"Which HTML tag is used for the document title?", "<title>", "<head>", "<header>", "<name>", "A", "The <title> tag defines the title of the HTML document."}
            };
            
            // Randomize question selection to avoid repetition on regenerate
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < htmlQuestions.length; i++) {
                indices.add(i);
            }
            java.util.Collections.shuffle(indices);
            
            for (int i = 0; i < numberOfQuestions && i < indices.size(); i++) {
                int questionIndex = indices.get(i);
                Map<String, String> question = new HashMap<>();
                question.put("questionText", htmlQuestions[questionIndex][0]);
                question.put("optionA", htmlQuestions[questionIndex][1]);
                question.put("optionB", htmlQuestions[questionIndex][2]);
                question.put("optionC", htmlQuestions[questionIndex][3]);
                question.put("optionD", htmlQuestions[questionIndex][4]);
                question.put("correctAnswer", htmlQuestions[questionIndex][5]);
                question.put("explanation", htmlQuestions[questionIndex][6]);
                questions.add(question);
            }
            
        } else if (topic.toLowerCase().contains("css")) {
            String[][] cssQuestions = {
                {"What does CSS stand for?", "Cascading Style Sheets", "Computer Style Sheets", "Creative Style Sheets", "Colorful Style Sheets", "A", "CSS stands for Cascading Style Sheets, used to style HTML elements."},
                {"Which tag is used to link an external CSS stylesheet?", "<link>", "<style>", "<css>", "<stylesheet>", "A", "The <link> tag with rel='stylesheet' is used to link external CSS files."},
                {"How do you insert a CSS comment?", "/*This is a comment*/", "//This is a comment", "'This is a comment'", "<!--This is a comment-->", "A", "CSS comments are written using /* comment */ syntax."},
                {"Which selector targets a specific HTML element?", "Element selector", "Class selector", "ID selector", "Universal selector", "A", "Element selectors target specific HTML tags like p, div, h1."},
                {"What is the correct syntax for adding a background color?", "background-color: red;", "color: background-red;", "bgcolor = 'red'", "background: red;", "A", "The background-color property sets the background color of an element."},
                {"Which property controls text color?", "color", "text-color", "font-color", "text-style", "A", "The color property is used to set the text color in CSS."},
                {"How do you change the font size?", "font-size: 16px;", "size: 16px;", "font-size: 16;", "text-size: 16px;", "A", "The font-size property controls the size of text with units like px, em, rem."},
                {"Which property controls the margin around an element?", "margin", "padding", "border", "spacing", "A", "The margin property controls the space outside an element's border."},
                {"What does the 'inline' display property do?", "Makes an element flow within the text", "Creates a block-level element", "Creates an inline-block element", "Hides the element", "A", "Inline elements flow within the text and don't start on new lines."},
                {"Which unit represents a relative font size?", "em", "px", "pt", "cm", "A", "The em unit is relative to the parent element's font size."},
                {"What does the 'float' property do?", "Makes an element move out of the document flow", "Creates a fixed-position element", "Changes the background color", "Sets the text alignment", "A", "Float removes elements from normal flow and positions them left or right."},
                {"What is the purpose of the 'display: none;' property?", "Hides the element completely", "Makes the element invisible", "Changes the element's color", "Makes the element bigger", "A", "Display: none completely removes the element from the layout."},
                {"What selector targets elements with a specific class?", ".class-name", "#class-name", "class=class-name", "*class-name", "A", "Class selectors use a dot (.) followed by the class name."},
                {"What selector targets all elements of a certain type?", "Element selector", "Universal selector", "ID selector", "Class selector", "A", "Element selectors target all elements of a specific HTML tag."},
                {"How do you specify the width of an element?", "width: 200px;", "size: 200px;", "dimension: 200px;", "width: 200;", "A", "The width property sets element width with units like px, %, em."}
            };
            
            // Randomize CSS question selection
            List<Integer> cssIndices = new ArrayList<>();
            for (int i = 0; i < cssQuestions.length; i++) {
                cssIndices.add(i);
            }
            java.util.Collections.shuffle(cssIndices);
            
            for (int i = 0; i < numberOfQuestions && i < cssIndices.size(); i++) {
                int questionIndex = cssIndices.get(i);
                Map<String, String> question = new HashMap<>();
                question.put("questionText", cssQuestions[questionIndex][0]);
                question.put("optionA", cssQuestions[questionIndex][1]);
                question.put("optionB", cssQuestions[questionIndex][2]);
                question.put("optionC", cssQuestions[questionIndex][3]);
                question.put("optionD", cssQuestions[questionIndex][4]);
                question.put("correctAnswer", cssQuestions[questionIndex][5]);
                question.put("explanation", cssQuestions[questionIndex][6]);
                questions.add(question);
            }
            
        } else if (topic.toLowerCase().contains("java")) {
            String[][] javaQuestions = {
                {"Which keyword is used to create a class in Java?", "class", "Class", "new", "create", "A", "The 'class' keyword is used to define a class in Java."},
                {"What is the main method signature in Java?", "public static void main(String[] args)", "public void main(String[] args)", "static void main(String[] args)", "public main(String[] args)", "A", "The main method must be public, static, void and take String array as parameter."},
                {"Which access modifier makes a member accessible only within the same class?", "private", "public", "protected", "default", "A", "The private access modifier restricts access to the same class only."},
                {"What is inheritance in Java?", "A mechanism to acquire properties from another class", "Creating multiple objects", "Method overloading", "Exception handling", "A", "Inheritance allows a class to inherit properties and methods from another class."}
            };
            
            for (int i = 0; i < numberOfQuestions && i < javaQuestions.length; i++) {
                Map<String, String> question = new HashMap<>();
                question.put("questionText", javaQuestions[i][0]);
                question.put("optionA", javaQuestions[i][1]);
                question.put("optionB", javaQuestions[i][2]);
                question.put("optionC", javaQuestions[i][3]);
                question.put("optionD", javaQuestions[i][4]);
                question.put("correctAnswer", javaQuestions[i][5]);
                question.put("explanation", javaQuestions[i][6]);
                questions.add(question);
            }
            
        } else if (topic.toLowerCase().contains("javascript") || topic.toLowerCase().contains("js")) {
            String[][] jsQuestions = {
                {"What is JavaScript primarily used for?", "Web development and interactivity", "Database management", "System administration", "Network security", "A", "JavaScript is primarily used for adding interactivity to web pages and web development."},
                {"How do you declare a variable in JavaScript?", "var, let, or const", "variable", "declare", "dim", "A", "Variables in JavaScript can be declared using var, let, or const keywords."},
                {"Which method is used to add an element to the end of an array?", "push()", "add()", "append()", "insert()", "A", "The push() method adds one or more elements to the end of an array."},
                {"What does '===' operator do in JavaScript?", "Strict equality comparison", "Assignment", "Loose equality", "Not equal", "A", "The '===' operator performs strict equality comparison without type conversion."},
                {"How do you create a function in JavaScript?", "function functionName() {}", "create function functionName()", "def functionName():", "function = functionName()", "A", "Functions in JavaScript are created using the 'function' keyword followed by the function name."}
            };
            
            for (int i = 0; i < numberOfQuestions && i < jsQuestions.length; i++) {
                Map<String, String> question = new HashMap<>();
                question.put("questionText", jsQuestions[i][0]);
                question.put("optionA", jsQuestions[i][1]);
                question.put("optionB", jsQuestions[i][2]);
                question.put("optionC", jsQuestions[i][3]);
                question.put("optionD", jsQuestions[i][4]);
                question.put("correctAnswer", jsQuestions[i][5]);
                question.put("explanation", jsQuestions[i][6]);
                questions.add(question);
            }
            
        } else if (topic.toLowerCase().contains("python")) {
            String[][] pythonQuestions = {
                {"What is Python?", "A high-level programming language", "A type of snake", "A web browser", "A database", "A", "Python is a high-level, interpreted programming language known for its simplicity."},
                {"How do you print text in Python?", "print()", "echo()", "write()", "display()", "A", "The print() function is used to output text in Python."},
                {"Which symbol is used for comments in Python?", "#", "//", "/*", "<!--", "A", "The # symbol is used for single-line comments in Python."},
                {"What data type is used for whole numbers in Python?", "int", "integer", "number", "whole", "A", "The int data type is used for whole numbers in Python."},
                {"How do you create a list in Python?", "[]", "{}", "()", "<>", "A", "Square brackets [] are used to create lists in Python."}
            };
            
            for (int i = 0; i < numberOfQuestions && i < pythonQuestions.length; i++) {
                Map<String, String> question = new HashMap<>();
                question.put("questionText", pythonQuestions[i][0]);
                question.put("optionA", pythonQuestions[i][1]);
                question.put("optionB", pythonQuestions[i][2]);
                question.put("optionC", pythonQuestions[i][3]);
                question.put("optionD", pythonQuestions[i][4]);
                question.put("correctAnswer", pythonQuestions[i][5]);
                question.put("explanation", pythonQuestions[i][6]);
                questions.add(question);
            }
            
        } else {
            // Enhanced web development questions for unknown topics - assume web development context
            String[][] webDevQuestions = {
                {"What does HTML stand for?", "HyperText Markup Language", "High Tech Modern Language", "Home Tool Markup Language", "Hyperlink and Text Markup Language", "A", "HTML stands for HyperText Markup Language, used to create web pages."},
                {"Which tag is used to create a heading in HTML?", "h1 to h6", "header", "head", "title", "A", "HTML heading tags range from h1 (largest) to h6 (smallest)."},
                {"What tag is used to create a paragraph in HTML?", "p", "para", "paragraph", "text", "A", "The p tag is used to create paragraphs in HTML."},
                {"Which tag is used to create an unordered list in HTML?", "ul", "ol", "list", "li", "A", "The ul tag creates an unordered (bulleted) list in HTML."},
                {"Which tag is used to create an ordered list in HTML?", "ol", "ul", "list", "order", "A", "The ol tag creates an ordered (numbered) list in HTML."},
                {"What does CSS stand for?", "Cascading Style Sheets", "Creative Style Sheets", "Computer Style Sheets", "Colorful Style Sheets", "A", "CSS stands for Cascading Style Sheets, used to style HTML elements."},
                {"What is the correct way to include a CSS file in HTML?", "link rel='stylesheet'", "style src", "css href", "import css", "A", "Use link tag with rel='stylesheet' to include external CSS files."},
                {"Which CSS property is used to change the text color?", "color", "text-color", "font-color", "background-color", "A", "The color property sets the text color in CSS."},
                {"What does JavaScript do?", "Adds interactivity to web pages", "Styles web pages", "Adds structure to web pages", "Organizes files on a computer", "A", "JavaScript adds interactivity and dynamic behavior to web pages."},
                {"How do you write Hello World in an alert box using JavaScript?", "alert('Hello World')", "msg('Hello World')", "print('Hello World')", "console.log('Hello World')", "A", "The alert() function displays a message box with the specified text."},
                {"What are the three main components of a website?", "HTML CSS JavaScript", "HTML PHP MySQL", "Java Python C++", "Images Videos Audio", "A", "HTML provides structure, CSS provides styling, and JavaScript provides interactivity."},
                {"What does API stand for?", "Application Programming Interface", "Advanced Programming Interface", "Application Process Interface", "Advanced Process Interface", "A", "API stands for Application Programming Interface, allowing different software to communicate."},
                {"What is a web server?", "A computer that stores website files", "A program that displays websites", "A type of internet connection", "A programming language", "A", "A web server is a computer system that stores and serves website files to users."},
                {"What is a domain name?", "A website's address", "A type of programming language", "A file type", "A web server's name", "A", "A domain name is the human-readable address used to access a website."},
                {"What is HTTP?", "Hypertext Transfer Protocol", "Hypertext Transmission Protocol", "Hypertext Transfer Procedure", "Hypertext Transmission Procedure", "A", "HTTP is the protocol used for transferring web pages over the internet."},
                {"What does HTTPS stand for?", "Hypertext Transfer Protocol Secure", "Hypertext Transmission Protocol Secure", "Hypertext Transfer Protocol Server", "Hypertext Transmission Protocol Server", "A", "HTTPS is the secure version of HTTP, encrypting data transmission."},
                {"What is a browser?", "A program that displays websites", "A type of internet connection", "A programming language", "A web server", "A", "A web browser is software that retrieves and displays web pages."},
                {"What is responsive web design?", "Designing websites that adapt to different screen sizes", "Designing websites that are only viewed on desktops", "Designing websites that use only HTML", "Designing websites that use only CSS", "A", "Responsive design ensures websites work well on all device sizes."},
                {"What is a framework?", "A collection of tools and libraries for web development", "A type of web server", "A programming language", "A type of browser", "A", "A framework provides pre-built tools and structure for development."},
                {"What is a library?", "A collection of pre-written code", "A type of web server", "A programming language", "A type of browser", "A", "A library is a collection of reusable code functions and modules."},
                {"What does XML stand for?", "Extensible Markup Language", "Executable Markup Language", "Extended Markup Language", "Extra Markup Language", "A", "XML is a markup language for storing and transporting data."},
                {"What does JSON stand for?", "JavaScript Object Notation", "Java Object Notation", "JavaScript Object Network", "Java Object Network", "A", "JSON is a lightweight data interchange format based on JavaScript syntax."},
                {"What is the purpose of a head tag in HTML?", "To provide metadata about the HTML document", "To create a heading", "To create a paragraph", "To create a link", "A", "The head tag contains metadata like title, links to CSS, and other document information."},
                {"Which tag is used to embed an image in HTML?", "img", "image", "picture", "photo", "A", "The img tag with src attribute embeds images in HTML pages."},
                {"What is a comment in HTML?", "Text ignored by the browser", "Text displayed in the browser", "A type of tag", "A type of style", "A", "HTML comments are notes in code that browsers don't display to users."},
                {"What is the purpose of a div tag?", "To create a division or section in an HTML document", "To create a paragraph", "To create a link", "To create a heading", "A", "The div tag creates containers for grouping and styling content."},
                {"What is the purpose of a span tag?", "To group inline elements", "To create a block-level element", "To create a heading", "To create a paragraph", "A", "The span tag groups inline elements for styling or scripting purposes."},
                {"What is the role of a web developer?", "To design and build websites", "To manage servers", "To write marketing materials", "To sell advertising", "A", "Web developers create and maintain websites and web applications."},
                {"Which HTML attribute is used to specify the source of an image?", "src", "href", "link", "source", "A", "The src attribute specifies the source URL of an image in HTML."},
                {"What is the correct HTML tag for creating a hyperlink?", "a", "link", "href", "url", "A", "The anchor tag 'a' with href attribute creates hyperlinks in HTML."},
                {"Which CSS property controls the spacing between elements?", "margin", "padding", "border", "spacing", "A", "The margin property controls the space outside an element's border."},
                {"What does the CSS property 'display: none' do?", "Hides the element completely", "Makes the element invisible", "Changes the element's color", "Makes the element bigger", "A", "Display: none completely removes the element from the layout."},
                {"Which JavaScript method is used to select an element by its ID?", "getElementById", "querySelector", "getElement", "selectById", "A", "The getElementById method selects an HTML element by its ID attribute."},
                {"What is the correct way to declare a variable in JavaScript?", "var myVariable", "variable myVariable", "declare myVariable", "dim myVariable", "A", "Variables in JavaScript can be declared using var, let, or const keywords."},
                {"Which HTML tag is used to create a table?", "table", "tab", "grid", "data", "A", "The table tag is used to create tables in HTML."},
                {"What is the purpose of the alt attribute in img tags?", "To provide alternative text for images", "To set the image alignment", "To specify the image size", "To add a border to the image", "A", "The alt attribute provides alternative text for images when they cannot be displayed."},
                {"Which CSS property is used to change the background color?", "background-color", "color", "bg-color", "background", "A", "The background-color property sets the background color of an element."},
                {"What is the correct HTML tag for the largest heading?", "h1", "heading", "head", "h6", "A", "h1 is the largest heading tag in HTML, with h6 being the smallest."},
                {"Which JavaScript operator is used for strict equality comparison?", "===", "==", "=", "!=", "A", "The === operator performs strict equality comparison without type conversion."},
                {"What is the purpose of the DOCTYPE declaration in HTML?", "To specify the HTML version", "To create a document title", "To link CSS files", "To add JavaScript", "A", "The DOCTYPE declaration tells the browser which version of HTML is being used."}
            };
            
            // Randomize question selection
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < webDevQuestions.length; i++) {
                indices.add(i);
            }
            java.util.Collections.shuffle(indices);
            
            for (int i = 0; i < numberOfQuestions && i < indices.size(); i++) {
                int questionIndex = indices.get(i);
                Map<String, String> question = new HashMap<>();
                
                // Ensure all fields are properly set and not null
                question.put("questionText", webDevQuestions[questionIndex][0] != null ? webDevQuestions[questionIndex][0] : "Sample Question");
                question.put("optionA", webDevQuestions[questionIndex][1] != null ? webDevQuestions[questionIndex][1] : "Option A");
                question.put("optionB", webDevQuestions[questionIndex][2] != null ? webDevQuestions[questionIndex][2] : "Option B");
                question.put("optionC", webDevQuestions[questionIndex][3] != null ? webDevQuestions[questionIndex][3] : "Option C");
                question.put("optionD", webDevQuestions[questionIndex][4] != null ? webDevQuestions[questionIndex][4] : "Option D");
                question.put("correctAnswer", webDevQuestions[questionIndex][5] != null ? webDevQuestions[questionIndex][5] : "A");
                question.put("explanation", webDevQuestions[questionIndex][6] != null ? webDevQuestions[questionIndex][6] : "No explanation provided");
                
                questions.add(question);
            }
        }
        
        return questions;
    }
}
