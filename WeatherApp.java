import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherApp {
    // API and configuration constants
    private static final String API_KEY = "13828b8798daaef3ccba7c6b8cbb55fe";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String JSON_FILE = "weather_data.json";

    // Thread pool for background tasks
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Mapping of keywords to symbols
    private static final Map<String, String> SUGGESTION_SYMBOLS = new HashMap<>() {{
        // Food and Drink
        put("eat", "🍽️");
        put("food", "🍔");
        put("cook", "🥘");
        put("restaurant", "🍣");
        put("drink", "🍹");
        put("coffee", "☕");

        // Activities
        put("walk", "🚶");
        put("run", "🏃");
        put("exercise", "💪");
        put("gym", "🏋️");
        put("bike", "🚲");
        put("swim", "🏊");
        put("hike", "🥾");

        // Work and Study
        put("work", "💼");
        put("study", "📚");
        put("meeting", "👥");
        put("project", "📊");
        put("report", "📝");

        // Home and Chores
        put("clean", "🧹");
        put("laundry", "🧺");
        put("groceries", "🛒");
        put("garden", "🌱");

        // Entertainment and Leisure
        put("movie", "🎬");
        put("music", "🎵");
        put("read", "📖");
        put("game", "🎮");
        put("shop", "🛍️");
        put("art", "🎨");

        // Travel and Outdoors
        put("travel", "✈️");
        put("trip", "🧳");
        put("vacation", "🏖️");
        put("camp", "⛺");

        // Personal Care
        put("sleep", "😴");
        put("relax", "🧘");
        put("health", "❤️");
        put("doctor", "🩺");

        // Technology
        put("computer", "💻");
        put("phone", "📱");
        put("code", "💻");
        put("email", "📧");

        // General
        put("plan", "📅");
        put("buy", "🛒");
        put("go", "🚀");
        put("check", "✅");
    }};

    // Weather condition to symbols mapping
    private static final Map<String, String> WEATHER_SYMBOLS = new HashMap<>() {{
        put("clear", "☀️");
        put("sunny", "☀️");
        put("cloud", "☁️");
        put("cloudy", "☁️");
        put("rain", "🌧️");
        put("shower", "🌧️");
        put("drizzle", "🌦️");
        put("snow", "❄️");
        put("mist", "🌫️");
        put("fog", "🌫️");
        put("thunder", "⚡");
        put("storm", "🌩️");
        put("wind", "💨");
        put("hot", "🔥");
        put("cold", "🧊");
        put("default", "🌤️");
    }};

    // Default symbols for when no keyword match is found
    private static final String[] DEFAULT_SYMBOLS = {
            "🌈", "🌟", "🍀", "🚀", "🔮", "🎲", "🧩", "🌻",
            "🦄", "🍁", "🌍", "🎈", "🦋", "🍄", "🎭"
    };

    // Weather icon mapping
    private static final Map<String, ImageIcon> WEATHER_ICONS = new HashMap<>();

    // Swing components
    private JFrame frame;
    private JTextField cityField;
    private JTextArea weatherArea;
    private DefaultListModel<String> suggestionsListModel;
    private JList<String> suggestionsList;

    // To-Do List components
    private DefaultListModel<String> todoListModel;
    private JList<String> todoList;
    private JTextField todoInputField;

    // Weather icon component
    private JLabel weatherIconLabel;
    private JProgressBar progressBar;

    private Random random;

    // Constructor
    public WeatherApp() {
        random = new Random();

        // Initialize weather icons
        initWeatherIcons();

        // Create main frame
        frame = new JFrame("Weather App");
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        Font customFont = new Font("Segoe UI Emoji", Font.PLAIN, 18);

        // Top panel with city input
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel();
        cityField = new JTextField(15);
        JButton getWeatherButton = new JButton("Get Weather");
        inputPanel.add(new JLabel("Enter Location:"));
        inputPanel.add(cityField);
        inputPanel.add(getWeatherButton);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        // Weather display area
        weatherArea = new JTextArea();
        weatherArea.setFont(customFont);
        weatherArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(weatherArea);

        // Weather icon label
        weatherIconLabel = new JLabel();
        weatherIconLabel.setHorizontalAlignment(JLabel.CENTER);
        weatherIconLabel.setPreferredSize(new Dimension(100, 100));

        // Panel to hold weather info and icon
        JPanel weatherPanel = new JPanel(new BorderLayout());
        weatherPanel.add(scrollPane, BorderLayout.CENTER);
        weatherPanel.add(weatherIconLabel, BorderLayout.EAST);

        // Create a split panel for suggestions and to-do list
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JPanel suggestionsPanel = createSuggestionsPanel();
        JPanel todoPanel = createTodoPanel();
        splitPane.setLeftComponent(suggestionsPanel);
        splitPane.setRightComponent(todoPanel);
        splitPane.setDividerLocation(400); // Equal split

        // Add components to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(weatherPanel, BorderLayout.CENTER);
        frame.add(splitPane, BorderLayout.SOUTH);

        // Action listeners
        getWeatherButton.addActionListener(e -> requestWeatherData(cityField.getText().trim()));

        // Add key listener to cityField to respond to Enter key
        cityField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    requestWeatherData(cityField.getText().trim());
                }
            }
        });

        // Load existing data
        loadSavedData();

        // Make frame visible
        frame.setVisible(true);
    }

    // Load all saved data
    private void loadSavedData() {
        try {
            File jsonFile = new File(JSON_FILE);
            if (jsonFile.exists()) {
                loadSuggestionsFromJSON();
                loadTodoItemsFromJSON();

                // Try to load last weather data
                String jsonContent = readFromFile(JSON_FILE);
                JSONObject jsonData = new JSONObject(jsonContent);

                if (jsonData.has("weather")) {
                    JSONObject weatherData = jsonData.getJSONObject("weather");
                    if (weatherData.has("city")) {
                        String city = weatherData.getString("city");
                        if (!city.isEmpty()) {
                            cityField.setText(city);

                            // Display last saved weather
                            if (weatherData.has("currentWeather") && weatherData.has("forecast")) {
                                String currentWeather = weatherData.getString("currentWeather");
                                String forecast = weatherData.getString("forecast");
                                String airPollution = weatherData.has("airPollution") ?
                                        weatherData.getString("airPollution") : "";

                                String weatherInfo = "📍 Location: " + city + "\n\n" +
                                        currentWeather + "\n" +
                                        forecast + "\n" +
                                        airPollution;

                                weatherArea.setText(weatherInfo);

                                // Extract weather condition for UI theming
                                String weatherCondition = "";
                                if (currentWeather.contains("Condition:")) {
                                    int startIndex = currentWeather.indexOf("Condition:") + 10;
                                    weatherCondition = currentWeather.substring(startIndex).trim();
                                }

                                // Update UI theme and icon
                                updateUITheme(weatherCondition);
                                ImageIcon icon = getWeatherIcon(weatherCondition);
                                weatherIconLabel.setIcon(icon);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Silent fail on loading - will create new data
        }
    }

    // Initialize weather icons with larger size for better visibility
    private void initWeatherIcons() {
        // Create icons with larger size (128x128 instead of 64x64)
        WEATHER_ICONS.put("clear", createIcon(Color.YELLOW, "☀️", 128));
        WEATHER_ICONS.put("sunny", createIcon(Color.YELLOW, "☀️", 128));
        WEATHER_ICONS.put("cloud", createIcon(Color.LIGHT_GRAY, "☁️", 128));
        WEATHER_ICONS.put("cloudy", createIcon(Color.LIGHT_GRAY, "☁️", 128));
        WEATHER_ICONS.put("rain", createIcon(new Color(100, 149, 237), "🌧️", 128));
        WEATHER_ICONS.put("shower", createIcon(new Color(100, 149, 237), "🌧️", 128));
        WEATHER_ICONS.put("snow", createIcon(Color.WHITE, "❄️", 128));
        WEATHER_ICONS.put("mist", createIcon(new Color(220, 220, 220), "🌫️", 128));
        WEATHER_ICONS.put("fog", createIcon(new Color(220, 220, 220), "🌫️", 128));
        WEATHER_ICONS.put("thunder", createIcon(new Color(70, 130, 180), "⚡", 128));
        WEATHER_ICONS.put("default", createIcon(Color.GRAY, "🌤️", 128));
    }

    // Create a simple icon with a background color and emoji with specified size
    private ImageIcon createIcon(Color color, String emoji, int size) {
        // Create an image with the specified size
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(color);
        g2d.fillOval(size/16, size/16, size*6/8, size*6/8);

        // Draw emoji with larger font size
        g2d.setColor(Color.orange);
        int fontSize = size/2; // Half the size of the image
        g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(emoji);
        int textHeight = fm.getHeight();
        g2d.drawString(emoji, (size - textWidth) /99, size/2 + textHeight/4);

        g2d.dispose();
        return new ImageIcon(image);
    }

    // Method to update UI colors based on weather conditions
    private void updateUITheme(String weatherCondition) {
        Color backgroundColor, foregroundColor, accentColor;

        weatherCondition = weatherCondition.toLowerCase();

        if (weatherCondition.contains("rain") || weatherCondition.contains("shower")) {
            backgroundColor = new Color(220, 230, 240); // Light blue-gray
            foregroundColor = new Color(40, 50, 60);    // Dark blue
            accentColor = new Color(30, 144, 255);      // Dodger blue
        } else if (weatherCondition.contains("snow")) {
            backgroundColor = new Color(245, 245, 255); // Very light blue
            foregroundColor = new Color(70, 70, 90);    // Dark slate
            accentColor = new Color(120, 150, 230);     // Periwinkle
        } else if (weatherCondition.contains("clear") || weatherCondition.contains("sunny")) {
            backgroundColor = new Color(255, 250, 230); // Light yellow
            foregroundColor = new Color(23, 22, 21);    // Dark brown
            accentColor = new Color(253, 227, 5);      // Gold
        } else if (weatherCondition.contains("cloud")) {
            backgroundColor = new Color(240, 240, 245); // Light gray
            foregroundColor = new Color(60, 60, 70);    // Dark gray
            accentColor = new Color(140, 160, 190);     // Steel blue
        } else if (weatherCondition.contains("fog") || weatherCondition.contains("mist")) {
            backgroundColor = new Color(230, 230, 230); // Lighter gray
            foregroundColor = new Color(80, 80, 80);    // Medium gray
            accentColor = new Color(180, 180, 190);     // Silver
        } else {
            backgroundColor = UIManager.getColor("Panel.background");
            foregroundColor = UIManager.getColor("Label.foreground");
            accentColor = new Color(0, 120, 215);       // Default blue
        }

        // Apply colors to components
        frame.getContentPane().setBackground(backgroundColor);
        weatherArea.setBackground(backgroundColor);
        weatherArea.setForeground(foregroundColor);

        cityField.setBackground(backgroundColor.brighter());
        cityField.setForeground(foregroundColor);

        // Update button colors
        for (Component comp : frame.getContentPane().getComponents()) {
            updateComponentColors(comp, backgroundColor, foregroundColor, accentColor);
        }

        suggestionsList.setBackground(backgroundColor);
        suggestionsList.setForeground(foregroundColor);
        todoList.setBackground(backgroundColor);
        todoList.setForeground(foregroundColor);
    }

    // Helper method to recursively update component colors
    private void updateComponentColors(Component comp, Color backgroundColor, Color foregroundColor, Color accentColor) {
        if (comp instanceof JButton) {
            ((JButton) comp).setBackground(accentColor);
            ((JButton) comp).setForeground(Color.BLACK);
        } else if (comp instanceof JPanel) {
            comp.setBackground(backgroundColor);
            for (Component child : ((JPanel) comp).getComponents()) {
                updateComponentColors(child, backgroundColor, foregroundColor, accentColor);
            }
        }
    }

    // Create suggestions panel
    private JPanel createSuggestionsPanel() {
        JPanel suggestionsPanel = new JPanel(new BorderLayout());
        suggestionsListModel = new DefaultListModel<>();
        suggestionsList = new JList<>(suggestionsListModel);
        suggestionsList.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JScrollPane suggestionsScroll = new JScrollPane(suggestionsList);

        // Remove suggestion button
        JButton removeSuggestionButton = new JButton("Remove Suggestion");
        removeSuggestionButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        // Suggestions control panel
        JPanel suggestionsControlPanel = new JPanel();
        suggestionsControlPanel.add(removeSuggestionButton);

        // Suggestions label
        JLabel suggestionsLabel = new JLabel("Weather-Based Suggestions:");
        suggestionsLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        // Arrange suggestions panel
        suggestionsPanel.add(suggestionsLabel, BorderLayout.NORTH);
        suggestionsPanel.add(suggestionsScroll, BorderLayout.CENTER);
        suggestionsPanel.add(suggestionsControlPanel, BorderLayout.SOUTH);

        // Action listeners
        removeSuggestionButton.addActionListener(e -> removeSuggestion(suggestionsList.getSelectedIndex()));

        return suggestionsPanel;
    }

    // Create To-Do panel
    private JPanel createTodoPanel() {
        JPanel todoPanel = new JPanel(new BorderLayout());
        todoListModel = new DefaultListModel<>();
        todoList = new JList<>(todoListModel);
        todoList.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JScrollPane todoScrollPane = new JScrollPane(todoList);

        // To-Do input panel
        JPanel todoInputPanel = new JPanel(new FlowLayout());
        todoInputField = new JTextField(20);
        JButton addTodoButton = new JButton("Add To-Do");
        addTodoButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JButton removeTodoButton = new JButton("Remove To-Do");
        removeTodoButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        todoInputPanel.add(new JLabel("To-Do:"));
        todoInputPanel.add(todoInputField);
        todoInputPanel.add(addTodoButton);
        todoInputPanel.add(removeTodoButton);

        // To-Do panel label
        JLabel todoLabel = new JLabel("My To-Do List:");
        todoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        // Arrange To-Do panel
        todoPanel.add(todoLabel, BorderLayout.NORTH);
        todoPanel.add(todoScrollPane, BorderLayout.CENTER);
        todoPanel.add(todoInputPanel, BorderLayout.SOUTH);

        // Add action listeners for To-Do list
        addTodoButton.addActionListener(e -> addTodoItem());
        removeTodoButton.addActionListener(e -> removeTodoItem());

        // Add key listener to respond to Enter key in the to-do input field
        todoInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addTodoItem();
                }
            }
        });

        return todoPanel;
    }

    // Method to find the most relevant symbol for a suggestion
    private String findMostRelevantSymbol(String suggestion) {
        // Convert suggestion to lowercase for case-insensitive matching
        String lowerSuggestion = suggestion.toLowerCase();

        // Check for keyword matches
        for (Map.Entry<String, String> entry : SUGGESTION_SYMBOLS.entrySet()) {
            if (lowerSuggestion.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // If no match found, return a random default symbol
        return DEFAULT_SYMBOLS[random.nextInt(DEFAULT_SYMBOLS.length)];
    }

    // Find the most relevant weather symbol for a condition
    private String findWeatherSymbol(String weatherCondition) {
        // Convert to lowercase for case-insensitive matching
        String lowerCondition = weatherCondition.toLowerCase();

        // Check for keyword matches in the weather condition
        for (Map.Entry<String, String> entry : WEATHER_SYMBOLS.entrySet()) {
            if (lowerCondition.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // If no match found, return the default weather symbol
        return WEATHER_SYMBOLS.get("default");
    }

    // Remove a suggestion from the list
    private void removeSuggestion(int index) {
        if (index != -1) {
            suggestionsListModel.remove(index);
            saveSuggestionsToJSON();
        }
    }

    // Add To-Do item method - UPDATED to use symbols
    private void addTodoItem() {
        String newTodo = todoInputField.getText().trim();
        if (!newTodo.isEmpty()) {
            // Find the most relevant symbol for this to-do item
            String symbol = findMostRelevantSymbol(newTodo);

            // Add the to-do item with the symbol
            todoListModel.addElement(symbol + " " + newTodo);
            todoInputField.setText("");
            saveTodoItemsToJSON();
        }
    }

    // Remove To-Do item method
    private void removeTodoItem() {
        int selectedIndex = todoList.getSelectedIndex();
        if (selectedIndex != -1) {
            todoListModel.remove(selectedIndex);
            saveTodoItemsToJSON();
        }
    }

    // Get weather icon based on condition
    private ImageIcon getWeatherIcon(String weatherCondition) {
        String condition = weatherCondition.toLowerCase();

        for (String key : WEATHER_ICONS.keySet()) {
            if (condition.contains(key)) {
                return WEATHER_ICONS.get(key);
            }
        }

        return WEATHER_ICONS.get("default");
    }

    // Format a weather text section with appropriate symbols
    private String formatWeatherTextWithSymbols(String text, String weatherCondition) {
        StringBuilder formatted = new StringBuilder();

        // Get the appropriate weather symbol for the condition
        String weatherSymbol = findWeatherSymbol(weatherCondition);

        // Split the text into lines
        String[] lines = text.split("\n");

        for (String line : lines) {
            // For temperature line
            if (line.contains("Temperature:")) {
                formatted.append("🌡 ").append(line).append("\n");
            }
            // For humidity line
            else if (line.contains("Humidity:")) {
                formatted.append("💧 ").append(line).append("\n");
            }
            // For wind line
            else if (line.contains("Wind:")) {
                formatted.append("🌬 ").append(line).append("\n");
            }
            // For condition line - use the dynamic weather symbol
            else if (line.contains("Condition:")) {
                formatted.append(weatherSymbol).append(" ").append(line).append("\n");
            }
            // For forecast lines
            else if (line.contains("Forecast:")) {
                formatted.append("📅 ").append(line).append("\n");
            }
            // For specific forecast day lines
            else if (line.contains("dt_txt")) {
                String forecastSymbol;
                // Try to extract the condition from this forecast line
                if (line.contains("-")) {
                    String forecastCondition = line.substring(line.lastIndexOf("-") + 1).trim();
                    forecastSymbol = findWeatherSymbol(forecastCondition);
                } else {
                    forecastSymbol = "📆";
                }
                formatted.append(forecastSymbol).append(" ").append(line).append("\n");
            }
            // For air pollution header
            else if (line.contains("Air Pollution Data:")) {
                formatted.append("\n🌍 Air Pollution Data:\n");
            }
            // For specific air pollution lines
            else if (line.contains("CO:") || line.contains("NO₂:") ||
                    line.contains("SO₂:") || line.contains("PM2.5:")) {
                // Use a specific symbol for each pollutant
                if (line.contains("CO:")) {
                    formatted.append("⚗️ ").append(line).append("\n");
                } else if (line.contains("NO₂:")) {
                    formatted.append("🧪 ").append(line).append("\n");
                } else if (line.contains("SO₂:")) {
                    formatted.append("💨 ").append(line).append("\n");
                } else if (line.contains("PM2.5:")) {
                    formatted.append("😷 ").append(line).append("\n");
                } else {
                    formatted.append(line).append("\n");
                }
            }
            // Default case - keep the line as is
            else {
                formatted.append(line).append("\n");
            }
        }

        return formatted.toString();
    }

    // Main method to request weather data and handle UI updates
    private void requestWeatherData(String city) {
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a location!");
            return;
        }

        // Show progress
        progressBar.setVisible(true);
        weatherArea.setText("Fetching weather data...");

        // Use executor service to run network operations off the EDT
        executorService.submit(() -> {
            try {
                // Get weather data
                final String currentWeather = fetchWeatherData(city, "weather");
                final String forecast = fetchWeatherData(city, "forecast");
                final String airPollution = fetchAirPollution(city);

                // Extract weather condition for UI theming and icon
                String weatherCondition = "";
                if (currentWeather.contains("Condition:")) {
                    int startIndex = currentWeather.indexOf("Condition:") + 10;
                    weatherCondition = currentWeather.substring(startIndex).trim();
                }

                // Format the weather text with appropriate symbols
                final String formattedCurrentWeather = formatWeatherTextWithSymbols(currentWeather, weatherCondition);
                final String formattedForecast = formatWeatherTextWithSymbols(forecast, weatherCondition);

                // Complete weather info
                final String finalWeatherInfo = "📍 Location: " + city + "\n\n" +
                        formattedCurrentWeather + "\n" +
                        formattedForecast + "\n" +
                        airPollution;

                // Final weather condition for theme update
                final String finalWeatherCondition = weatherCondition;

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Update weather display
                        weatherArea.setText(finalWeatherInfo);

                        // Update UI theme based on weather
                        updateUITheme(finalWeatherCondition);

                        // Update weather icon
                        ImageIcon icon = getWeatherIcon(finalWeatherCondition);
                        weatherIconLabel.setIcon(icon);

                        // Generate suggestions based on current weather
                        generateWeatherSuggestions(formattedCurrentWeather);

                        // Save all data
                        saveWeatherToJSON(city, formattedCurrentWeather, formattedForecast, airPollution);

                        // Hide progress bar
                        progressBar.setVisible(false);
                    } catch (Exception ex) {
                        handleError("Error updating UI: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                final String errorMsg = ex.getMessage();
                SwingUtilities.invokeLater(() -> {
                    handleError("Error: " + errorMsg);
                });
            }
        });
    }

    // Error handler method
    private void handleError(String message) {
        progressBar.setVisible(false);

        if (message.contains("400") || message.contains("404")) {
            weatherArea.setText("Location not found. Please check the spelling and try again.");
        } else if (message.contains("401")) {
            weatherArea.setText("API key issue. Please check your API key configuration.");
        } else if (message.contains("429")) {
            weatherArea.setText("Too many requests. Please try again later.");
        } else if (message.contains("Connection")) {
            weatherArea.setText("Network error. Please check your internet connection and try again.");
        } else {
            weatherArea.setText("Error: " + message);
        }
    }

    // Generate weather-based suggestions
    private void generateWeatherSuggestions(String currentWeather) {
        // Clear previous suggestions
        suggestionsListModel.clear();

        // Parse weather conditions from the currentWeather string
        String lowercaseWeather = currentWeather.toLowerCase();

        // Check for different weather conditions and temperature ranges
        if (lowercaseWeather.contains("rain") || lowercaseWeather.contains("shower")) {
            suggestionsListModel.addElement("☔ Bring an umbrella");
            suggestionsListModel.addElement("🧥 Wear waterproof clothing");
            suggestionsListModel.addElement("🏠 Indoor activities recommended");
        }

        if (lowercaseWeather.contains("snow")) {
            suggestionsListModel.addElement("🧤 Wear warm gloves and hat");
            suggestionsListModel.addElement("🧣 Dress in layers for warmth");
            suggestionsListModel.addElement("⛄ Good day for winter activities");
        }

        if (lowercaseWeather.contains("clear") || lowercaseWeather.contains("sunny")) {
            suggestionsListModel.addElement("😎 Wear sunglasses and sunscreen");
            suggestionsListModel.addElement("🏖️ Great day for outdoor activities");
            suggestionsListModel.addElement("🥤 Stay hydrated");
        }

        if (lowercaseWeather.contains("cloud")) {
            suggestionsListModel.addElement("📸 Good lighting for photography");
            suggestionsListModel.addElement("🚶 Pleasant day for walking");
        }

        if (lowercaseWeather.contains("fog") || lowercaseWeather.contains("mist")) {
            suggestionsListModel.addElement("🚗 Drive carefully - reduced visibility");
            suggestionsListModel.addElement("🔦 Use fog lights when driving");
        }

        // Temperature-based suggestions
        // Temperature-based suggestions
        if (lowercaseWeather.contains("temperature")) {
            // Extract temperature value
            int tempStartIndex = lowercaseWeather.indexOf("temperature:") + 12;
            int tempEndIndex = lowercaseWeather.indexOf("°", tempStartIndex);

            if (tempStartIndex > 12 && tempEndIndex > tempStartIndex) {
                try {
                    String tempStr = lowercaseWeather.substring(tempStartIndex, tempEndIndex).trim();
                    double tempCelsius = Double.parseDouble(tempStr);

                    if (tempCelsius > 30) {
                        suggestionsListModel.addElement("🧊 Stay in shade and cool areas");
                        suggestionsListModel.addElement("🧴 Apply sunscreen regularly");
                        suggestionsListModel.addElement("🏊 Consider swimming if possible");
                    } else if (tempCelsius > 25) {
                        suggestionsListModel.addElement("🏞️ Nice weather for outdoor activities");
                        suggestionsListModel.addElement("🧢 Wear a hat for sun protection");
                    } else if (tempCelsius < 5) {
                        suggestionsListModel.addElement("🧣 Bundle up with warm clothes");
                        suggestionsListModel.addElement("☕ Enjoy hot drinks");
                    } else if (tempCelsius < 15) {
                        suggestionsListModel.addElement("🧥 Wear a light jacket");
                    }
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // In case of parsing error, add generic suggestions
                    suggestionsListModel.addElement("📱 Check weather updates regularly");
                }
            }
        }

        // Wind-based suggestions
        if (lowercaseWeather.contains("wind")) {
            suggestionsListModel.addElement("🪁 Secure loose items outdoors");

            // Try to extract wind speed
            int windStartIndex = lowercaseWeather.indexOf("wind:") + 5;
            int windEndIndex = lowercaseWeather.indexOf("m/s", windStartIndex);

            if (windStartIndex > 5 && windEndIndex > windStartIndex) {
                try {
                    String windStr = lowercaseWeather.substring(windStartIndex, windEndIndex).trim();
                    double windSpeed = Double.parseDouble(windStr);

                    if (windSpeed > 10) {
                        suggestionsListModel.addElement("🌪️ Be cautious of strong winds");
                    } else if (windSpeed > 5) {
                        suggestionsListModel.addElement("🧥 Wear windproof clothing");
                    }
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // If parsing fails, add generic wind suggestion
                    suggestionsListModel.addElement("🌬️ Be aware of wind conditions");
                }
            }
        }

        // Always add generic suggestions
        suggestionsListModel.addElement("📅 Plan activities according to weather");
        suggestionsListModel.addElement("📱 Check updates for weather changes");

        // Save suggestions to JSON
        saveSuggestionsToJSON();
    }

    // Fetch weather data from OpenWeatherMap API
    private String fetchWeatherData(String city, String endpoint) throws Exception {
        // Format the URL for API call
        String urlStr = BASE_URL + endpoint + "?q=" + city + "&appid=" + API_KEY + "&units=metric";

        // Create URL and open connection
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Check response code
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API request failed with response code: " + responseCode);
        }

        // Read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());

        if (endpoint.equals("weather")) {
            return parseCurrentWeather(jsonResponse);
        } else if (endpoint.equals("forecast")) {
            return parseForecast(jsonResponse);
        }

        return "No data available";
    }

    // Parse current weather data from JSON response
    private String parseCurrentWeather(JSONObject jsonResponse) {
        StringBuilder weatherInfo = new StringBuilder();

        // Main weather data
        JSONObject main = jsonResponse.getJSONObject("main");
        double temperature = main.getDouble("temp");
        int humidity = main.getInt("humidity");

        // Weather condition
        JSONArray weatherArray = jsonResponse.getJSONArray("weather");
        JSONObject weather = weatherArray.getJSONObject(0);
        String condition = weather.getString("main") + " - " + weather.getString("description");

        // Wind data
        JSONObject wind = jsonResponse.getJSONObject("wind");
        double windSpeed = wind.getDouble("speed");

        // Format weather information
        weatherInfo.append("Temperature: ").append(temperature).append("°C\n");
        weatherInfo.append("Humidity: ").append(humidity).append("%\n");
        weatherInfo.append("Wind: ").append(windSpeed).append(" m/s\n");
        weatherInfo.append("Condition: ").append(condition).append("\n");

        return weatherInfo.toString();
    }

    // Parse forecast data from JSON response
    private String parseForecast(JSONObject jsonResponse) {
        StringBuilder forecastInfo = new StringBuilder();
        forecastInfo.append("Forecast:\n");

        // Get forecast list
        JSONArray forecastList = jsonResponse.getJSONArray("list");

        // Number of forecast entries to show (every 8 entries is approximately a day)
        int numberOfEntries = Math.min(forecastList.length(), 5);

        // Iterate through forecast entries
        for (int i = 0; i < numberOfEntries; i++) {
            JSONObject forecast = forecastList.getJSONObject(i * 8); // Every 8th entry (24 hours)

            // Get timestamp
            String timestamp = forecast.getString("dt_txt");

            // Main weather data
            JSONObject main = forecast.getJSONObject("main");
            double temperature = main.getDouble("temp");

            // Weather condition
            JSONArray weatherArray = forecast.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String condition = weather.getString("main") + " - " + weather.getString("description");

            // Format forecast entry
            forecastInfo.append("  ").append(timestamp)
                    .append(" - ").append(temperature).append("°C - ")
                    .append(condition).append("\n");
        }

        return forecastInfo.toString();
    }

    // Fetch air pollution data
    private String fetchAirPollution(String city) {
        try {
            // First get coordinates for the city
            String urlStr = BASE_URL + "weather" + "?q=" + city + "&appid=" + API_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return ""; // Silent fail for air pollution
            }

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse coordinates
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject coord = jsonResponse.getJSONObject("coord");
            double lat = coord.getDouble("lat");
            double lon = coord.getDouble("lon");

            // Now fetch air pollution with coordinates
            String pollutionUrlStr = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" +
                    lat + "&lon=" + lon + "&appid=" + API_KEY;
            URL pollutionUrl = new URL(pollutionUrlStr);
            HttpURLConnection pollutionConnection = (HttpURLConnection) pollutionUrl.openConnection();
            pollutionConnection.setRequestMethod("GET");

            // Check response code
            int pollutionResponseCode = pollutionConnection.getResponseCode();
            if (pollutionResponseCode != 200) {
                return ""; // Silent fail
            }

            // Read response
            reader = new BufferedReader(new InputStreamReader(pollutionConnection.getInputStream()));
            response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse air pollution data
            return parseAirPollution(new JSONObject(response.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Silent fail on air pollution
        }
    }

    // Parse air pollution data
    private String parseAirPollution(JSONObject jsonResponse) {
        StringBuilder pollutionInfo = new StringBuilder();
        pollutionInfo.append("Air Pollution Data:\n");

        try {
            JSONArray list = jsonResponse.getJSONArray("list");
            if (list.length() > 0) {
                JSONObject pollution = list.getJSONObject(0);
                JSONObject components = pollution.getJSONObject("components");

                // Extract pollutant values
                double co = components.getDouble("co");
                double no2 = components.getDouble("no2");
                double so2 = components.getDouble("so2");
                double pm25 = components.getDouble("pm2_5");

                // Format pollution information
                pollutionInfo.append("CO: ").append(co).append(" μg/m³\n");
                pollutionInfo.append("NO₂: ").append(no2).append(" μg/m³\n");
                pollutionInfo.append("SO₂: ").append(so2).append(" μg/m³\n");
                pollutionInfo.append("PM2.5: ").append(pm25).append(" μg/m³\n");
            } else {
                pollutionInfo.append("No air pollution data available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            pollutionInfo.append("Error parsing air pollution data.");
        }

        return pollutionInfo.toString();
    }

    // Save weather data to JSON file
    private void saveWeatherToJSON(String city, String currentWeather, String forecast, String airPollution) {
        try {
            // Create JSON object with weather data
            JSONObject jsonData = readJSONFileOrCreateNew();

            // Add or update weather information
            JSONObject weatherObj = new JSONObject();
            weatherObj.put("city", city);
            weatherObj.put("currentWeather", currentWeather);
            weatherObj.put("forecast", forecast);
            weatherObj.put("airPollution", airPollution);

            jsonData.put("weather", weatherObj);

            // Write to file
            writeToFile(JSON_FILE, jsonData.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save suggestions to JSON file
    private void saveSuggestionsToJSON() {
        try {
            // Create JSON array with suggestions
            JSONObject jsonData = readJSONFileOrCreateNew();
            JSONArray suggestionsArray = new JSONArray();

            // Add all suggestions to array
            for (int i = 0; i < suggestionsListModel.size(); i++) {
                suggestionsArray.put(suggestionsListModel.get(i));
            }

            jsonData.put("suggestions", suggestionsArray);

            // Write to file
            writeToFile(JSON_FILE, jsonData.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save To-Do items to JSON file
    private void saveTodoItemsToJSON() {
        try {
            // Create JSON array with To-Do items
            JSONObject jsonData = readJSONFileOrCreateNew();
            JSONArray todoArray = new JSONArray();

            // Add all To-Do items to array
            for (int i = 0; i < todoListModel.size(); i++) {
                todoArray.put(todoListModel.get(i));
            }

            jsonData.put("todoItems", todoArray);

            // Write to file
            writeToFile(JSON_FILE, jsonData.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load suggestions from JSON file
    private void loadSuggestionsFromJSON() {
        try {
            // Read JSON file
            JSONObject jsonData = readJSONFileOrCreateNew();

            // Check if suggestions exist
            if (jsonData.has("suggestions")) {
                JSONArray suggestionsArray = jsonData.getJSONArray("suggestions");

                // Clear current list
                suggestionsListModel.clear();

                // Add all suggestions to list
                for (int i = 0; i < suggestionsArray.length(); i++) {
                    suggestionsListModel.addElement(suggestionsArray.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load To-Do items from JSON file
    private void loadTodoItemsFromJSON() {
        try {
            // Read JSON file
            JSONObject jsonData = readJSONFileOrCreateNew();

            // Check if To-Do items exist
            if (jsonData.has("todoItems")) {
                JSONArray todoArray = jsonData.getJSONArray("todoItems");

                // Clear current list
                todoListModel.clear();

                // Add all To-Do items to list
                for (int i = 0; i < todoArray.length(); i++) {
                    todoListModel.addElement(todoArray.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read JSON file or create new JSON object
    private JSONObject readJSONFileOrCreateNew() {
        try {
            File file = new File(JSON_FILE);
            if (file.exists()) {
                String content = readFromFile(JSON_FILE);
                return new JSONObject(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return empty JSON object if file doesn't exist or error occurs
        return new JSONObject();
    }

    // Write string to file
    private void writeToFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }

    // Read string from file
    private String readFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    // Main method
    public static void main(String[] args) {
        try {
            // Set Look and Feel to system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create application on EDT
        SwingUtilities.invokeLater(() -> new WeatherApp());
    }
}