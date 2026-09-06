# FaceCollage AI ✨

FaceCollage AI is a 100% on-device Android application that processes portrait videos, detects and groups unique individuals across multiple appearances, and automatically generates a stunning, shareable collage featuring the "best shot" of each person.

Built as an advanced solution for the **Video-based unique-person collage** assignment.

## 🚀 Features

- **Advanced 3-Stage ML Pipeline**: Utilizes Face Detection, Deep Metric Learning (Embeddings), and Graph-Based Clustering entirely on-device to track identities without any backend.
- **Smart "Best Shot" Selector**: Evaluates every frame of a person based on sharpness, smile probability, eye-openness, frontality, and bounding box area to guarantee the most photogenic, high-quality hero shot for the collage.
- **Dynamic Collage Studio**: Offers multiple highly-polished collage formats (Modern Grid, Polaroid Board, Story Poster) inspired by Instagram Stories.
- **Save & Share**: Instantly export the generated collage to the device gallery or share it via the standard Android share sheet.
- **Privacy First**: 100% offline processing. No video frames or biometric data ever leave the device.

---

## 🧠 Machine Learning Architecture

The identity grouping accuracy is the core of this application. To achieve the 50% accuracy requirement (perfectly grouping 5 unique people across 20+ appearances in complex whip-pan videos), the app employs a state-of-the-art on-device pipeline.

### 1. Face Detection (Google ML Kit)
- **Engine**: Google ML Kit Vision.
- **Filtering**: A strict 5-landmark requirement (eyes, nose, mouth corners) naturally filters out false positives and half-faces. Adaptive sharpness gates prevent heavily motion-blurred faces from polluting the clustering pool, while size components ensure large, prominent faces are prioritized.

### 2. Face Embedding (FaceNet-512)
- **Model Used**: **FaceNet-512 (TFLite)**
- **Input**: 160x160 aligned face bitmaps.
- **Output**: 512-dimensional L2-normalized floating-point vector.
- **Why FaceNet?**: After extensive A/B testing against ArcFace, FaceNet-512 proved vastly superior at maintaining high inter-class variance (keeping different people separate) while handling extreme lighting and angle changes in the provided test videos.

### 3. Clustering (Reciprocal KNN + Chinese Whispers)
Simple hierarchical clustering fails on video data due to extreme pose variations creating "bridge" embeddings between different people. To solve this, we implemented a custom graph-clustering algorithm:
- **Reciprocal K-Nearest Neighbors (K=12)**: Edges are only formed if Face A is in Face B's top 12 matches, AND Face B is in Face A's top 12. This strictly prevents different people from merging.
- **Chinese Whispers Label Propagation**: An ultra-fast graph clustering algorithm that naturally discovers the number of unique people without needing to pre-define `k`.
- **Centroid Merge Threshold (`0.50`)**: A post-processing pass reunites clusters of the same person that were split by extreme pose changes (e.g., split-screen shots vs solo shots). The algorithm requires a minimum cosine similarity of **`0.50`**, strictly validated against cross-member average pairwise similarity to prevent identity collapse.

### 4. Counting Appearances
An appearance is defined as a continuous visible segment. The system temporally sorts all frames for a unique person. Any gap larger than **`800ms`** between detections constitutes a broken segment, incrementing the appearance count.

---

## 🛠️ Build & Setup Instructions

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17+
- Android Device or Emulator running API Level 26 (Android 8.0) or higher. (A physical device is recommended for ML inference performance).

### Steps to Run
1. Clone this repository.
2. Open the project in Android Studio.
3. Allow Gradle to sync and download the required dependencies (Jetpack Compose, ML Kit, TensorFlow Lite).
4. Build and Run the app on your device.
5. Grant the requested Media/Storage permissions when prompted.
6. Select one of the test portrait videos (Sample 1, 2, or 3) from your gallery to begin processing.

---

## 💻 Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose, Material 3
- **Media**: Media3 (ExoPlayer) for frame extraction and playback
- **Machine Learning**: Google ML Kit (Face Detection), TensorFlow Lite (FaceNet Embeddings)
- **Concurrency**: Kotlin Coroutines (Dispatchers.Default / IO)
