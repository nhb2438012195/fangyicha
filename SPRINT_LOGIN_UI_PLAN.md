# Sprint Plan: 登录页面 UI 改造

> 基于设计文档: `docs/superpowers/specs/2026-05-25-login-ui-design.md`
> 当前分支: `购房功能`

---

## 目标

将 LoginView.vue 从居中卡片布局（蓝色主题）改造为左右分栏布局（60% 品牌区 + 40% 登录卡片），采用暖橙住宅风格配色，仅修改模板和样式层，保留所有脚本逻辑不变。

---

## 涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/views/login/LoginView.vue` | 修改 | 仅改 `<template>` 和 `<style scoped>`，不动 `<script setup>` |

**不修改的文件：**
- `App.vue` — 全局样式不动，所有改造通过作用域样式隔离
- `RegisterView.vue` — 本次不涉及
- 任何 API、Store、Router 文件 — 逻辑不动
- 全局 Element Plus 主题覆盖（App.vue 中的 `--el-button-bg-color` 等）— 注册页改造时会统一处理，本次登录页通过 scoped 样式覆盖

---

## 具体修改步骤

### Step 1: 替换 `<template>` 结构

将现有的单卡片结构：

```html
<div class="login-page">
  <div class="login-card">
    <div class="login-header">...</div>
    <el-alert .../>
    <el-form ...>...</el-form>
    <div class="login-footer">...</div>
  </div>
</div>
```

替换为左右分栏结构：

```html
<div class="login-page">
  <!-- 左侧品牌区 60% -->
  <div class="login-brand">
    <!-- 几何装饰圆形 -->
    <div class="deco-circle deco-circle-1"></div>
    <div class="deco-circle deco-circle-2"></div>
    <div class="deco-circle deco-circle-3"></div>
    <div class="deco-circle deco-circle-4"></div>
    <!-- 装饰弧线 -->
    <div class="deco-arc deco-arc-1"></div>
    <div class="deco-arc deco-arc-2"></div>
    <!-- 品牌内容 -->
    <div class="login-brand-content">
      <div class="brand-logo">
        <span class="brand-logo-icon">房</span>
      </div>
      <h1 class="brand-title">房易查</h1>
      <p class="brand-slogan">让购房更简单</p>
    </div>
  </div>

  <!-- 右侧登录卡片区 40% -->
  <div class="login-card-wrapper">
    <div class="login-card">
      <div class="login-card-header">
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-subtitle">请登录您的账号</p>
      </div>

      <!-- 错误提示 -->
      <el-alert .../>

      <!-- 登录表单 -->
      <el-form ...>...</el-form>

      <!-- 注册链接 -->
      <div class="login-footer">...</div>
    </div>
  </div>
</div>
```

各部分模板细节：

**1a. 左侧品牌区 `.login-brand`**
- `v-bind` 无需绑定任何数据，纯静态内容
- `.deco-circle-1` ~ `.deco-circle-4`: 四个半透明圆形，白色 `rgba(255,255,255,0.12~0.18)`，尺寸 300~600px
- `.deco-arc-1`、`.deco-arc-2`: 两条柔和弧线，通过 `border` + `border-radius` 模拟
- `.brand-logo`: "房"字在暖色方块中（白色半透明背景或深褐色背景）
- `.brand-title`: "房易查" 大号白色/深褐色文字
- `.brand-slogan`: "让购房更简单" 深褐色小字

**1b. 右侧登录卡片区 `.login-card-wrapper`**
- 使用 `display: flex; align-items: center; justify-content: center;` 使卡片垂直水平居中
- `.login-card` 宽度固定 400px，暖白背景 `#fdf8f3`
- 卡片内部标题改为 "欢迎回来" / "请登录您的账号"
- 角色切换按钮、表单、登录按钮、注册链接位置不变，但配色更新

**1c. 表单内容保持不变的要素：**
- `v-model`、`ref` 绑定不变
- `el-form`、`el-form-item` 结构不变
- `el-radio-group` + `el-radio-button` 角色切换逻辑不变
- `el-input` 的 `prefix-icon` 引用 `User`/`Lock` 不变
- `handleLogin` 事件绑定不变
- `@keyup.enter` 不变
- `show-password` 属性不变

### Step 2: 替换 `<style scoped>` 样式

删除所有现有样式，替换为新样式。具体分段：

**2a. 页面容器 `.login-page`**
```css
.login-page {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
```

**2b. 左侧品牌区 `.login-brand`**
```css
.login-brand {
  position: relative;
  flex: 0 0 60%;
  background: linear-gradient(135deg, #f5a623, #d4a373);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
```

**2c. 几何装饰圆**
```css
.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.deco-circle-1 {
  width: 600px; height: 600px;
  top: -150px; right: -100px;
}
.deco-circle-2 {
  width: 400px; height: 400px;
  bottom: -80px; left: -60px;
  background: rgba(255, 255, 255, 0.08);
}
.deco-circle-3 {
  width: 300px; height: 300px;
  top: 20%; left: 15%;
  background: rgba(255, 255, 255, 0.06);
}
.deco-circle-4 {
  width: 500px; height: 500px;
  bottom: 10%; right: 5%;
  background: rgba(255, 255, 255, 0.1);
}
```

**2d. 装饰弧线**
- 使用伪元素或独立 div + 宽高 + border-bottom + border-radius 实现柔和弧线
- 半透明白色，覆盖在圆形之间

**2e. 品牌内容**
```css
.login-brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}
.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}
.brand-logo-icon {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36px;
  font-weight: 700;
}
.brand-title {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: 4px;
}
.brand-slogan {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  letter-spacing: 2px;
}
```

**2f. 右侧卡片区**
```css
.login-card-wrapper {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f0eb;  /* 过渡色，或直接使用 #fdf8f3 满铺 */
}
.login-card {
  width: 400px;
  background: #fdf8f3;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(180, 130, 80, 0.12);
}
```

**2g. 卡片头部**
```css
.login-card-header {
  margin-bottom: 32px;
}
.card-title {
  font-size: 24px;
  font-weight: 700;
  color: #4a3728;
  margin: 0 0 8px;
}
.card-subtitle {
  font-size: 14px;
  color: #8a7a6a;
  margin: 0;
}
```

**2h. 表单元素样式（覆盖 Element Plus 默认蓝色主题）**
```css
/* Input 聚焦时暖橙边框 */
.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid #e8ddd0;
  box-shadow: none;
  background: #fff;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #f5a623;
  box-shadow: 0 0 0 1px #f5a623;
}
.login-form :deep(.el-input__inner) {
  color: #4a3728;
}

/* 角色切换按钮 - 覆盖 el-radio-button 默认蓝色 */
.login-form :deep(.el-radio-button__inner) {
  border-color: #e8ddd0;
  color: #8a7a6a;
  background: #fff;
}
.login-form :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: #f5a623;
  border-color: #f5a623;
  color: #fff;
  box-shadow: none;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  background: #f5a623;
  border-color: #f5a623;
  color: #fff;
}
.login-btn:hover {
  background: #e0961a;
  border-color: #e0961a;
}
```

**2i. 错误提示、注册链接**
```css
.login-error {
  margin-bottom: 20px;
}
.login-footer {
  text-align: center;
  color: #8a7a6a;
  font-size: 14px;
  margin-top: 24px;
}
.register-link {
  color: #f5a623;
  text-decoration: none;
  font-weight: 500;
}
.register-link:hover {
  text-decoration: underline;
}

/* 表单间距 */
.login-form {
  margin-bottom: 0;
}
.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}
```

### Step 3: 验证与对照清单

修改完成后逐项对照设计文档：

- [ ] 左右比例 60:40
- [ ] 满屏高度 100vh，无内边距
- [ ] 左侧背景渐变色 `#f5a623` → `#d4a373`
- [ ] 4 个装饰圆形（不同尺寸、位置、透明度）
- [ ] 2 条装饰弧线在圆形之间
- [ ] 品牌 Logo：72px "房"字暖色方块
- [ ] 品牌名 "房易查" 42px 白色粗体
- [ ] 标语 "让购房更简单" 白色小字
- [ ] 右侧背景暖白 `#fdf8f3`
- [ ] 卡片宽 400px，圆角 16px，阴影 `0 8px 32px rgba(180, 130, 80, 0.12)`
- [ ] 卡片内边距 40px
- [ ] 标题 "欢迎回来" 24px 粗体 / "请登录您的账号" 14px
- [ ] 色值全部正确（见配色表）
- [ ] 角色切换按钮选中时暖橙色
- [ ] Input 聚焦时暖橙边框
- [ ] 登录按钮暖橙填充，悬停 `#e0961a`
- [ ] 注册链接暖橙色
- [ ] 脚本逻辑无任何改动

### Step 4: 运行验证

```bash
# 启动开发服务器
cd frontend
npm run dev

# 手动测试：
# 1. 访问登录页，确认左右分栏布局渲染正确
# 2. 切换角色按钮，确认样式变更为暖橙
# 3. 输入/聚焦 input，确认边框色变为暖橙
# 4. 提交空表单，确认校验提示正常
# 5. 输入错误凭证，确认错误提示 el-alert 正常显示
# 6. 输入正确凭证，确认登录跳转正常
# 7. 点击"立即注册"，确认跳转到注册页面
# 8. 缩放到不同分辨率（1280px / 1440px / 1920px），确认布局适配
```

---

## 验收标准

### 视觉验收
1. 登录页呈现左右两栏布局，无拼接缝隙或错位
2. 左侧渐变过渡平滑，无硬色带
3. 装饰圆形成次分明，不遮挡品牌文字可读性
4. 品牌文字清晰醒目，与背景对比度足够
5. 右侧卡片视觉突出，阴影柔和自然
6. 所有颜色与设计文档配色表完全一致
7. 表单元素在聚焦/选中/悬停状态均有对应暖橙反馈

### 功能验收
1. 表单校验正常触发（必填、密码长度）
2. 角色切换正确更新 `loginForm.role`
3. 登录按钮 loading 状态正常
4. 错误提示可关闭
5. 登录成功后根据角色跳转对应 dashboard
6. 注册链接可跳转 `/register`

### 代码验收
1. `<script setup>` 部分零修改
2. 所有新样式通过 `scoped` 隔离，不影响其他页面
3. 没有硬编码值（颜色使用常量风格，集中在样式头部声明）
4. 没有删除或破坏现有的任何功能逻辑

---

## 风险与注意事项

| 风险 | 缓解措施 |
|------|----------|
| `:deep()` 选择器污染其他页面 | 确认父选择器 `.login-form` 限定在 scoped 范围内 |
| 装饰圆形影响响应式 | 圆形使用 `position: absolute`，不参与布局流 |
| Element Plus 全局主题变量覆盖登录页样式 | `<style scoped>` 优先级高于全局样式，无影响 |
| 左右两栏在窄屏上显示异常 | 设计稿面向桌面端（登录页），暂不考虑 <1024px 适配 |
