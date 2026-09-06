# FaceCollage AI ✨

FaceCollage AI is a 100% on-device Android application that processes portrait videos, detects and groups unique individuals across multiple appearances, and automatically generates a stunning, shareable collage featuring the "best shot" of each person.

Built as a production-grade solution for the **Video-based unique-person collage** assignment, powered by an advanced offline ML pipeline and a modern **Cinematic Luxe Dark** interface designed with **Stitch via MCP in Google Antigravity**.

---

## 🚀 Key Features

- **100% On-Device & Offline**: Zero cloud dependencies, complete biometric privacy, and instantaneous local processing.
- **Advanced 3-Stage ML Pipeline**: Combines Google ML Kit face detection, FaceNet-512 deep metric learning, and reciprocal graph clustering.
- **Smart "Best Shot" Selector**: Multi-metric objective quality scoring (sharpness, smile confidence, eye-openness, frontality, and resolution) to choose the optimal hero shot for each person.
- **Dynamic Collage Studio**: Clean, text-free photo tiles across multiple aspect ratios (Modern Grid, Polaroid Board, and 9:16 Vertical Story Poster).
- **Hardware-Accelerated Export**: Real-time canvas rendering with 1-tap gallery saving (`Pictures/UniquePersonCollage`) and native Android share sheet integration.

---

## 🧠 Machine Learning Architecture

Achieving high-accuracy identity grouping across video sequences with motion blur, fast whip-pans, and extreme pose variations requires more than basic distance thresholding. FaceCollage AI employs an enterprise-grade ML architecture:

```
[Video Frame (Media3)]
         │
         ▼
[Google ML Kit Vision] ──> Strict 5-landmark gate (eyes, nose, mouth) + sharpness filter
         │
         ▼
[FaceNet-512 TFLite]   ──> 160×160 aligned crop ──> 512-D L2-normalized unit sphere vector
         │
         ▼
[Reciprocal KNN Graph] ──> Mutual Top-12 neighbor constraint (eliminates transitive drift)
         │
         ▼
[Chinese Whispers]     ──> Unsupervised label propagation (discovers unique person count)
         │
         ▼
[Centroid Merge Pass]  ──> Minimum cosine similarity 0.50 with cross-validation gate
         │
         ▼
[Best Shot Selector]   ──> Sharpness + Smile + Eye-Open + Head-Euler-Angle ranking
```

### 1. Face Detection & Quality Filtering (Google ML Kit)
- **Engine**: Google ML Kit Vision Face Detector.
- **Strict Landmark Gate**: Faces are accepted only when both eyes, nose base, and mouth corners are detected. This automatically rejects partial faces, profile silhouettes, and false positives.
- **Adaptive Sharpness & Size Gates**: Faces with bounding box sizes below 80×80 or severe motion blur are discarded before embedding to protect cluster purity.

### 2. Deep Metric Learning (FaceNet-512)
- **Model Architecture**: FaceNet Inception-ResNet-v1 (TFLite, 512-dimensional output).
- **Preprocessing**: 160×160 RGB bitmap normalization `(pixel - 127.5) / 128.0`.
- **L2 Unit Sphere Normalization**: Every output embedding is normalized such that $\|v\|_2 = 1.0$, allowing cosine similarity to be computed via fast dot products.
- **Why FaceNet-512 over ArcFace Mobile**: In comprehensive A/B testing on multi-person video datasets, FaceNet-512 exhibited a tighter intra-class distribution and significantly wider inter-class margin, preventing distinct people from collapsing into single clusters.

### 3. Graph-Based Clustering (Reciprocal KNN + Chinese Whispers)
Standard agglomerative or hierarchical clustering fails on video streams because gradual pose shifts create transitive "bridge" embeddings between different identities.

- **Reciprocal K-Nearest Neighbors ($K = 12$)**: An edge between face $A$ and face $B$ is created **if and only if** $A \in \text{TopK}(B)$ and $B \in \text{TopK}(A)$. This mutual consensus requirement cleanly severs accidental bridge connections.
- **Chinese Whispers Label Propagation**: An ultra-fast, parameter-free graph clustering algorithm that naturally discovers the true number of unique people in $O(V + E)$ time without requiring a pre-specified cluster count $k$.
- **Centroid Merge Pass ($Threshold = 0.50$)**: Resolves cluster splits caused by extreme lighting or angle transitions (e.g. solo close-up vs split-screen wide shot) with cross-member pairwise similarity validation.

### 4. Temporal Appearance Counting
An "appearance" is defined as a continuous, sustained visibility window. The system temporally sorts all detection timestamps for a unique person:
- Gaps $\le 800\text{ms}$ are treated as the same continuous scene appearance.
- Gaps $> 800\text{ms}$ signify that the individual left and re-entered the frame, incrementing the appearance counter.

### 5. Multi-Factor "Best Shot" Selector
Rather than picking the first or largest face, every face crop in a cluster is evaluated using a composite quality formula:

$$\text{Quality Score} = 0.35 \times S_{\text{sharpness}} + 0.25 \times P_{\text{smile}} + 0.20 \times \bar{O}_{\text{eyes}} + 0.10 \times F_{\text{frontality}} + 0.10 \times A_{\text{area}}$$

- **$S_{\text{sharpness}}$**: Laplacian variance on grayscale bitmap.
- **$P_{\text{smile}}$**: ML Kit smile probability $[0.0, 1.0]$.
- **$\bar{O}_{\text{eyes}}$**: Average of left and right eye-open probabilities $[0.0, 1.0]$.
- **$F_{\text{frontality}}$**: Deviation penalty based on head Euler angles $(Y, Z)$.
- **$A_{\text{area}}$**: Normalized face crop bounding box resolution.

---

## 🎨 UI & UX Revamp with Stitch via Antigravity MCP

The application's interface was completely transformed from a basic prototype into a state-of-the-art, cinematic studio experience. This revamp was architected and generated directly inside **Google Antigravity** using **Stitch MCP (Model Context Protocol)** servers.

### How Antigravity & Stitch MCP Were Utilized
1. **Design System Discovery**: Connected Antigravity directly to Stitch's design system server (`projects/1817737176593474940`) via MCP tools (`list_screens`, `get_screen`, `create_design_system`).
2. **Cinematic Luxe Dark Palette**: Implemented custom HSL-tailored tokens in Jetpack Compose:
   - **Obsidian Dark Surface**: `#06080F` & `#0D1527`
   - **Electric Violet Accent**: `#B68CFF` & `#4D21B2`
   - **Golden Amber Studio Glow**: `#FFB95F` & `#774300`
   - **Precision Cyan Telemetry**: `#7EE787` & `#38BDF8`
3. **Zero ML Disruption Guarantee**: The entire visual layer was refactored strictly at the presentation boundary, ensuring the 512-D neural clustering and ML Kit landmark detection pipelines remained 100% untouched and deterministic.

### Revamped Screens & Modules

```
┌─────────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────────┐
│     1. Home Import      │ ──> │    2. Processing HUD    │ ──> │   3. Studio Results     │
│  HUD Viewfinder + Scan  │     │ Live Reticle + Stages   │     │ Metrics + Story Avatars │
└─────────────────────────┘     └─────────────────────────┘     └─────────────────────────┘
                                                                             │
                                                                             ▼
                                                                ┌─────────────────────────┐
                                                                │    4. Collage Studio    │
                                                                │ Grid · Polaroid · Story │
                                                                └─────────────────────────┘
```

#### 1. Home / Video Import Viewfinder
- **Cyber-Optical Viewfinder**: High-tech camera framing with 2dp corner brackets (`HudCorner`), animated glowing cyan scanline, and real-time telemetry pill overlays (`REC 60P`, `512-D EMB`).
- **One-Tap Video Ingestion**: Smooth gallery picker launcher integrated with hardware MediaStore.
- **Privacy & Security Trust Badge**: Balanced 2-line security pill assuring users that all biometric processing runs 100% locally on-device.

#### 2. Video Processing & Detection HUD
- **Real-Time Frame Preview**: Live ExoPlayer frame decoder feed with dynamic face tracking bounding box overlays.
- **Mathematical Vector Checklist**: Precise 4-stage pipeline progression indicator:
  - `Stage 1: Frame Ingestion & Sampling` (adaptive FPS)
  - `Stage 2: 5-Point Landmark Face Detection` (ML Kit Vision)
  - `Stage 3: 512-D Neural Feature Embedding` (FaceNet TFLite)
  - `Stage 4: Reciprocal KNN Graph Clustering` (Chinese Whispers)
- **Telemetry Bar**: Live frame counter, detections per second, and battery-optimized GPU acceleration indicator.

#### 3. Unique People Studio Results
- **2-Column Analytical Metrics Bar**: Instant readout of unique individuals identified, processing velocity (ms/frame), and cluster confidence.
- **Interactive Sorting**: Dynamic toggle between "Most Appearances" (descending order) and "Earliest Chronological Detection".
- **Story Avatar Rings**: Instagram/TikTok-inspired gradient rings highlighting the hero "best shot" with candidate thumbnail filmstrip inspection.
- **Non-Destructive Curation**: In-place soft-hide and cluster merge options allowing full user control.

#### 4. Dynamic Collage Studio & Export
- **Modern Grid**: Clean, high-resolution edge-to-edge photo tiles with rounded corners and dark slate styling.
- **Polaroid Board**: Realistic analog polaroid frames with organic rotational jitter, authentic drop shadows, and warm film tones.
- **Story Poster (9:16 Vertical Cinema)**:
  - Built specifically for Instagram Stories, TikTok, and vertical mobile wallpapers.
  - Double gold editorial frame insets, serif headline typography ("THE ENSEMBLE"), and custom studio billing credits.
  - Symmetrical multi-hero mosaic (prominent top cards + balanced lower tier) with pristine, text-free face crops.
- **Hardware-Accelerated Canvas Rendering**: Exports crisp 1080p/4K bitmaps directly to `Pictures/UniquePersonCollage` and integrates with the native Android system share sheet.

---

## 🛠️ Build & Setup Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Version 17+
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 34 (Android 14)
- **Hardware**: Physical Android device recommended for optimal TFLite NNAPI/GPU inference.

### Steps to Run
1. Clone this repository:
   ```bash
   git clone https://github.com/Priyansh-dabhi/video-based-unique-person-collage.git
   ```
2. Open the project in Android Studio.
3. Verify that `app/src/main/assets/facenet_512.tflite` is present.
4. Sync Gradle and build the project:
   ```bash
   ./gradlew assembleDebug
   ```
5. Deploy to your connected device or emulator.
6. Grant storage/media permissions when prompted, select any portrait video from your gallery, and watch the real-time detection and collage generation in action.

---

## 💻 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9+ |
| **UI Toolkit** | Jetpack Compose, Material 3, Custom Canvas Hardware 2D |
| **Design System** | Cinematic Luxe Dark (Created with Stitch MCP in Google Antigravity) |
| **Face Detection** | Google ML Kit Vision Face Detector (5-point Landmark Pipeline) |
| **Metric Learning** | TensorFlow Lite, FaceNet-512 (Inception-ResNet-v1) |
| **Clustering** | Custom Graph Reciprocal KNN + Chinese Whispers |
| **Media Extraction** | AndroidX Media3 (ExoPlayer), MediaMetadataRetriever |
| **Concurrency** | Kotlin Coroutines (`Dispatchers.Default`, `Dispatchers.IO`), StateFlow |

---

## 📄 License
Developed for the **Video-based unique-person collage** engineering assignment. All machine learning inference runs 100% locally and privately on-device.
