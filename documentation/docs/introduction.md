---
sidebar_position: 1
---

# Introduction

Welcome to **Kakao Compose** tutorial. Let's make UI testing easies and simple. In this documentation we will try to explain functionality of Kakao Compose and
fundamental concepts of implementation, how to add this library to your project and
how to start works on UI test step by step.

But before we started, don't forget that `KakaoCup` is fully Open Source and supported by community. 
Please don't hesitate to contribute to this documentation or fix any bugs. 
We always welcome to new contributors with any level of codding skills.

### Overview

Kakao Compose Multiplatform ships as a single module, with following dependencies

```mermaid
flowchart TD
%% Nodes
    KCompose("compose-multiplatform")
    AEspresso("Espresso")
    ACompose("Android Compose")

%% Connections
    KCompose --> ACompose 
    KCompose --> AEspresso

%% Style
    style AEspresso color:#FFFFFF, fill:#AA00FF, stroke:#AA00FF
    style ACompose color:#FFFFFF, fill:#AA00FF, stroke:#AA00FF

```
* Core
  * `compose-multiplatform` - Core module, which providing DSL wrapping for Espresso methods, `ComposeScreen` page objects and `KNode`
