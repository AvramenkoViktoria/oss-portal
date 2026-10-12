@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param String next = "/"
@template.layout.page(ctx = ctx, title = "Вхід", content = @`
    <div class="form-narrow">
        <h1>Вхід</h1>
        <form class="card" method="post" action="/login">
            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
            <input type="hidden" name="next" value="${next}">
            <div class="field">
                <label for="email">Електронна пошта</label>
                <input id="email" name="email" type="email" required autofocus autocomplete="username">
            </div>
            <div class="field">
                <label for="password">Пароль</label>
                <input id="password" name="password" type="password" required autocomplete="current-password">
            </div>
            <button class="btn" type="submit">Увійти</button>
        </form>
        <p class="muted small" style="margin-top:1rem">Немає облікового запису? <a href="/register">Зареєструватися</a></p>
    </div>
`)
