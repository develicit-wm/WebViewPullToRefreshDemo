# WebView Pull-to-Refresh Demo

Android WebView에서 Pull-to-Refresh를 구현하는 데모 프로젝트입니다.
Jetpack Compose + Material2 `pullRefresh`를 사용하여 네이티브 Pull-to-Refresh와 WebView 내부 스크롤 간의 충돌을 해결하는 방법을 보여줍니다.

## 기술 스택

- **Kotlin** 1.9.20
- **Jetpack Compose** (BOM 2024.01.00)
- **Material3** (Scaffold, TopAppBar)
- **Material2** (pullRefresh)
- **AndroidView** (WebView 래핑)
- **Android Gradle Plugin** 8.2.0
- **minSdk** 24 / **targetSdk** 34

## 프로젝트 구조

```
app/src/main/
├── java/com/demo/webviewptr/
│   └── MainActivity.kt          # Compose UI + WebView + Pull-to-Refresh
├── assets/
│   └── editor.html               # WebView에 로드되는 HTML (게시글 작성 폼)
├── res/
│   └── values/strings.xml
└── AndroidManifest.xml
```

## 핵심 구현

### Pull-to-Refresh + WebView 연동

```kotlin
// Material2 pullRefresh modifier로 WebView를 감싸는 구조
Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
    AndroidView(factory = { WebView(it).apply { ... } })
    PullRefreshIndicator(refreshing, pullRefreshState)
}
```

### JavaScript Bridge

WebView 내부 스크롤 상태와 에디터 포커스를 네이티브로 전달합니다.

```kotlin
addJavascriptInterface(object {
    @JavascriptInterface
    fun onScrollChanged(atTop: Boolean) { ... }

    @JavascriptInterface
    fun onEditorFocusChanged(focused: Boolean) { ... }
}, "AndroidBridge")
```

## 테스트 시나리오

HTML 페이지에 다양한 중첩 스크롤 시나리오가 포함되어 있습니다:

1. **스크롤 가능한 리스트** (overflow-y)
2. **가로 스크롤 카드** (overflow-x)
3. **Textarea** 내부 스크롤
4. **이중 중첩 스크롤** (nested overflow)
5. **채팅형 스크롤** (reverse scroll)
6. **코드 블록** (양방향 스크롤)
7. **iframe** 내부 스크롤

## 빌드 및 실행

```bash
# 빌드
./gradlew assembleDebug

# 기기에 설치 및 실행
./gradlew installDebug
adb shell am start -n com.demo.webviewptr/.MainActivity
```

## 라이선스

This project is for demonstration purposes.
