# A股虚拟交易系统（后端）

## 项目简介

本项目为A股虚拟盘的后端实现，专注于模拟中国A股市场的交易、持仓、盈亏统计等核心逻辑。用户可通过远程API获取实时行情，并在本地虚拟账户中自由买入/卖出股票，系统自动记录所有交易、持仓和资金变动。

## 后端核心能力

### 1. 实时行情对接
- 提供接口对接第三方A股行情API，获取股票最新价格
- 支持灵活扩展行情源

### 2. 虚拟交易撮合
- 支持市价买入/卖出，默认有对手盘，操作即成交
- 交易结果即时反映到账户和持仓

### 3. 账户与持仓管理
- 账户余额、股票持仓、交易流水等数据持久化存储
- 查询当前余额、持仓、历史交易

### 4. 盈亏统计
- 自动统计每笔交易、每只股票及整体账户的盈亏
- 支持按日期区间、股票代码等维度查询盈亏

### 5. 事务与一致性
- 所有买卖操作采用数据库事务，确保资金、持仓、流水三者强一致

## 技术架构

- 语言：Java 8+
- 数据库：SQLite（单文件嵌入式，便于开发和测试）
- 架构分层：
  - Resource层：RESTful API接口（可拓展）
  - Repository层：数据库操作，事务封装
  - Service逻辑可按需扩展

## 主要接口说明

- `StockTradingResource`
  - `buyStock(String symbol, int quantity, double price)`：买入指定股票
  - `sellStock(String symbol, int quantity, double price)`：卖出指定股票
  - `getBalance()`：查询账户余额
  - `getPosition(String symbol)`：查询某只股票持仓
- `StockTradingRepository`
  - 负责所有数据库操作和事务封装

## 运行与开发

### 环境要求
- Java 8 及以上
- Maven 3.6+

### 启动方式
```bash
mvn clean install
# 可通过单元测试验证主要逻辑
mvn test
```

### 代码结构
```
src/main/java/edu/unl/stock/
    StockTradingResource.java      # 交易核心逻辑
    StockTradingRepository.java    # 数据持久化与事务
    ...
```

## 测试与扩展
- 支持JUnit单元测试，覆盖买入、卖出、异常场景等
- 可扩展行情API适配器、风控规则、更多统计分析等

## 许可证

MIT License. 详见 [LICENSE](LICENSE)。

