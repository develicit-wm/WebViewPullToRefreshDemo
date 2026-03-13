# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Android WebView + Pull-to-Refresh 데모 앱. Jetpack Compose 기반. WebView 내부의 다양한 중첩 스크롤 시나리오에서 Pull-to-Refresh가 올바르게 동작하는지 테스트하는 것이 목적.

## 빌드

```bash
./gradlew assembleDebug
./gradlew installDebug  # 기기 연결 필요
```

테스트나 린트 설정은 없음 (순수 데모 프로젝트).

## 아키텍처

단일 Activity, 단일 화면 앱. 모든 UI 로직이 `app/src/main/java/com/demo/webviewptr/MainActivity.kt`에 있음.

- **Compose 레이어**: `Scaffold` > `Column` > `Box(pullRefresh)` > `AndroidView(WebView)` 구조
- **Pull-to-Refresh**: Material2 `pullRefresh` modifier + `PullRefreshIndicator` 사용 (Material3 아님)
- **WebView ↔ Native 통신**: `JavascriptInterface`로 `AndroidBridge` 객체를 WebView에 주입
  - JS → Native: `AndroidBridge.onScrollChanged(atTop)`, `AndroidBridge.onEditorFocusChanged(focused)`
  - Native → JS: `webView.evaluateJavascript("onRefreshed()", null)`
- **HTML 콘텐츠**: `app/src/main/assets/editor.html` — 게시글 작성 폼 + 7가지 중첩 스크롤 테스트 섹션 (리스트, 가로스크롤, textarea, 이중중첩, 채팅, 코드블록, iframe)

## 코딩 컨벤션

- Kotlin, Jetpack Compose 사용. XML 레이아웃 사용하지 않음
- WebView 콘텐츠는 `assets/` 내 HTML로 관리
- 한국어 UI, 한국어 주석 허용

## 의존성

- Kotlin 1.9.20, Compose Compiler 1.5.5
- Compose BOM 2024.01.00
- AGP 8.2.0, minSdk 24, targetSdk 34
