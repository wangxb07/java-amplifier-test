# Exception Test Amplifier

A prototype implementation of **test amplification for validating exception handling code**, inspired by the paper:

**"Amplifying Tests to Validate Exception Handling Code"**  
_ICSE 2012 - Pingyu Zhang & Sebastian Elbaum_

## 📌 Project Overview

This project demonstrates how to automatically amplify test cases to explore the **space of exceptional behaviors** in Java programs interacting with external resources.

Key goals:
- Detect faults in exception handling code
- Simulate various exception-throwing patterns for resource APIs
- Support automatic mocking and runtime anomaly detection

## 🏗️ Architecture Modules

### 1. `SequenceCollector`
- Instruments target classes and captures external resource API invocations during test execution
- Output: ordered sequence of resource calls per test case

### 2. `ExceptionalSpaceBuilder`
- Builds all possible mocking patterns for a bounded number of API calls
- For example: `[normal, exception, normal]`

### 3. `ResourceMocker` (AspectJ)
- Mocks resource API to throw exceptions based on configured mocking patterns
- Currently supports exceptions like `IOException`, `TimeoutException`, etc.

### 4. `TestExplorer`
- Replays test cases under each mocking pattern
- Detects anomalies: uncaught exceptions, abnormal termination, null dereference, etc.

## ▶️ Running the Project

### Requirements

- Java 8+
- Maven
- AspectJ (`ajc` compiler)

### Compile and Run Tests

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

## 📂 Project Structure

```
exception-test-amplifier/
├── pom.xml
├── aspect/
│   └── ExceptionMockAspect.aj
├── src/
│   ├── main/java/edu/unl/exceptionamplifier/
│   │   ├── collector/SequenceCollector.java
│   │   ├── builder/ExceptionalSpaceBuilder.java
│   │   ├── model/ApiCall.java
│   │   ├── model/MockingPattern.java
│   │   ├── mocker/ResourceMocker.java
│   │   ├── explorer/TestExplorer.java
│   │   └── util/ExceptionUtils.java
│   └── test/java/edu/unl/exceptionamplifier/testcases/
│       ├── ResourceClient.java
│       └── ResourceClientTest.java
```

---

## 🚀 Example Usage

在 `ResourceClientTest` 中添加对资源异常的测试：

```java
@Test
public void testResourceException() {
    ResourceMocker mocker = new ResourceMocker();
    mocker.mockResourceException("FileInputStream", "IOException");
    // 调用被 mock 的资源方法，验证异常处理路径
}
```
运行所有测试：
```bash
mvn test
```
你可以通过修改 `ExceptionMockAspect.aj`，自定义哪些资源调用会被 mock 为抛出异常。

---

## 📖 References

- Zhang, P., & Elbaum, S. (2012). Amplifying Tests to Validate Exception Handling Code. *ICSE 2012*. [PDF](https://web.archive.org/web/20160304195203/http://cse.unl.edu/~elbaum/icse12.pdf)
- AspectJ Documentation: https://www.eclipse.org/aspectj/doc/next/progguide/index.html

---

## 📝 License

MIT License. See [LICENSE](LICENSE) for details.
