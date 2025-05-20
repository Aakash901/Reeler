# Reeler - Social Media Screen Time Manager


Reeler is an Android application designed to help you manage your screen time on popular social media platforms by automating content scrolling and tracking your usage statistics. Take control of your digital wellbeing with customizable limits and usage insights.

## 🌟 Features

- **Automated Scrolling**: Hands-free scrolling through content on popular social media platforms
- **Multiple Platform Support**: Works with Instagram Reels, YouTube Shorts, LinkedIn Videos, and Snapchat Stories
- **Customizable Limits**: Set daily content consumption limits to manage your screen time
- **Ad Skipping**: Option to automatically skip sponsored content
- **Usage Statistics**: Track and visualize your content consumption habits
- **Adjustable Scroll Intervals**: Customize the timing between content scrolls
- **Clean, Modern UI**: User-friendly interface with dark theme support

## 📱 Supported Platforms

- **Instagram Reels**: Automated scrolling through Instagram's short-form video content
- **YouTube Shorts**: Navigate YouTube's TikTok-like short video section
- **LinkedIn Videos**: Scroll through video content in your LinkedIn feed
- **Snapchat Stories**: Automatically view stories without manual tapping

## 📊 Statistics Tracking

Reeler keeps track of your daily content consumption with detailed statistics:

- Content viewed per day across all platforms
- Time spent watching content
- Ads automatically skipped
- Remaining content allowance before reaching your set daily limit
- Visual charts and graphs to understand your usage patterns

## 🔧 How It Works

Reeler leverages Android's Accessibility Service framework to:

1. **Monitor App Usage**: Detect when you open a supported social media app
2. **Navigate to Content**: Automatically find and tap on the relevant content section (Reels, Shorts, etc.)
3. **Manage Scrolling**: Implement smart timing for scrolling through content
4. **Detect Sponsored Content**: Identify and optionally skip advertisements
5. **Track Statistics**: Record usage data for each platform
6. **Enforce Limits**: Stop automatic scrolling when your daily limit is reached

## 🛠️ Technical Implementation

- **Accessibility Service**: Core functionality based on Android's Accessibility framework
- **Coroutines**: Asynchronous operations for smooth user experience
- **MVVM Architecture**: Clean separation of UI and business logic
- **Room Database**: Local storage for usage statistics
- **ViewPager2**: Smooth navigation between statistics screens
- **Material Design**: Modern UI components and animations

## 💻 Installation

1. Clone the repository
   ```bash
   git clone https://github.com/Aakash901/Reeler.git
   ```

2. Open the project in Android Studio

3. Build and run the application on your device

4. Enable Accessibility Service permission when prompted

## 📝 Usage Guide

### Setting Up

1. **Select Platform**: Choose which social media platform you want to use
2. **Set Scroll Interval**: Adjust how long to view each content item (in seconds)
3. **Set Daily Limit**: Choose how many items you want to view per day
4. **Toggle Ad Skipping**: Enable or disable automatic skipping of sponsored content

### Starting Automation

1. Tap the green PLAY button to start the service
2. Reeler will open the selected app and navigate to the content section
3. Sit back as the app automatically scrolls through content at your chosen interval
4. The service will stop when your daily limit is reached

### Viewing Statistics

The home screen displays statistics about your usage including:
- Current day's content consumption
- Remaining content before reaching your limit
- Charts showing usage patterns
- Tap on the graph for more detailed statistics

### Stopping Automation

- Tap the red STOP button to immediately stop auto-scrolling
- The service will automatically stop when reaching your daily limit

## ✨ Smart Features

- **Pre-analysis**: Analyzes upcoming content before scrolling to ensure smooth transitions
- **Sponsored Content Detection**: Identifies ads using content description patterns
- **Adaptive Timing**: Adjusts scroll timing based on content loading status
- **UI State Verification**: Ensures UI has updated before proceeding with next operations

## 🔒 Privacy & Permissions

Reeler requires:
- Accessibility Service permission: To detect and interact with social media apps
- Internet permission: For basic functionality

Reeler does NOT:
- Collect or transmit any personal data
- Access your content beyond what's necessary for scrolling
- Require root access

## 🔍 Troubleshooting

- **Service Stops Unexpectedly**: Ensure battery optimization is disabled for Reeler
- **Content Not Scrolling**: Check if the app is using the latest version of supported social media apps
- **Statistics Not Updating**: Restart the application and try again

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📬 Contact

Email : aakash.kr.singh0102@gmail.com

---

<p align="center">
  Made with ❤️ for better digital wellbeing
</p>
