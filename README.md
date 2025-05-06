# 异常测试放大器

本项目是一个用于验证异常处理代码的测试放大原型实现，灵感来源于以下论文：

《异常测试放大器》

## 项目简介

本项目演示了如何自动放大测试用例，系统性地探索 Java 程序与外部资源交互时的异常行为空间。

主要目标：
- 检测异常处理代码中的缺陷
- 模拟资源 API 抛出各种异常的模式
- 支持自动 Mock 及运行时异常检测

## 架构模块

### 1. `SequenceCollector`
- 对目标类进行插桩，捕获测试期间的外部资源 API 调用
- 输出：每个测试用例对应的有序资源调用序列

### 2. `ExceptionalSpaceBuilder`
- 针对有限数量的 API 调用，构建所有可能的 Mocking Pattern
- 例如: `[normal, exception, normal]`

### 3. `ResourceMocker` (AspectJ)
- 基于配置的 Mocking Pattern，对资源 API 进行异常注入（使用 AspectJ 切面）
- 当前支持如 `IOException`、`TimeoutException` 等异常类型

### 4. `TestExplorer`
- 在每种 Mocking Pattern 下回放测试用例
- 检测异常：未捕获异常、异常终止、空指针解引用等

## 项目运行指南

### 环境要求

- Java 8+
- Maven
- AspectJ (`ajc` 编译器)

### 编译与运行测试

```bash
mvn clean install
mvn test
```

> 若需编译并织入 AspectJ 切面，可用：
> 
> ```bash
> mvn clean compile
> ```

---

## 项目结构

```
exception-test-amplifier/
├── pom.xml
├── aspect/
│   └── ExceptionMockAspect.aj
├── src/
│   ├── main/java/edu/unl/exceptionamplifier/
│   │   ├── collector/SequenceCollector.java
│   │   ├── builder/ExceptionalSpaceBuilder.java
│   │   ├── mocker/ResourceMocker.java
│   │   ├── explorer/TestExplorer.java
│   │   └── resource/...
│   └── test/java/edu/unl/exceptionamplifier/testcases/...
```

---

## 使用示例

在 `ResourceClientTest` 中添加对资源异常的测试：

```java
@Test
public void testIoFileReaderException() throws Exception {
    IoFileReaderResource resource = new IoFileReaderResource();
    try {
        resource.readFile("not_exist.txt");
        fail("应抛出 IOException");
    } catch (IOException e) {
        // 期望捕获异常
    }
}
```

---

## 参考文献

- AspectJ 官方文档: https://www.eclipse.org/aspectj/doc/next/progguide/index.html

---

## 许可证

MIT License. 详见 [LICENSE](LICENSE)。
