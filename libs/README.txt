libs フォルダについて
=====================

このフォルダには、ビルド時に参照する第三者MODのJARを「自分のPC上だけ」で配置します。
JARはGitHubへコミットしません。本プロジェクトにも再配布しません。

1.0.16-rc1のコンパイルで想定する正確なファイル名
-----------------------------------------------
TILM-1.20.1-5.0.0-Forge.jar
LMML-1.20.1-8.0.5-Forge.jar
LMRBCompat-1.20.1-1.1.0-Forge.jar
GVCLib__1.20.1__beta6.1.jar
HandmadeGuns2__1.20.1__beta1.3.jar
GVCObjModel__1.20.1__2.4.jar

入手方法
--------
各MOD作者の正式配布元から取得してください。
主な参照先は THIRD_PARTY_NOTICES.txt に記載しています。

注意
----
・ファイル名を変えた場合は build.gradle 側も合わせて変更してください。
・runClientを使う場合は、TILM/LMML/LMRB等が必要とするArchitectury、Cloth Config、
  LittleMaid ReBirth、Kotlin for Forgeなど、実際のゲーム環境に必要な前提MODも
  開発環境へ用意する必要があります。
