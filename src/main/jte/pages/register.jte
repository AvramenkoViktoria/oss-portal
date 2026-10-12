@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@template.layout.page(ctx = ctx, title = "Реєстрація", content = @`
    <div class="form-narrow">
        <h1>Реєстрація</h1>
        <p class="muted">Обліковий запис потрібен, щоб подавати петиції. Читати протоколи можна без входу.</p>
        <form class="card" method="post" action="/register">
            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
            <div class="field">
                <label for="fullName">Прізвище та ім’я</label>
                <input id="fullName" name="fullName" type="text" required autofocus>
            </div>
            <div class="field">
                <label for="email">Електронна пошта</label>
                <input id="email" name="email" type="email" required autocomplete="username">
            </div>
            <div class="field">
                <label for="password">Пароль (щонайменше 8 символів)</label>
                <input id="password" name="password" type="password" minlength="8" required autocomplete="new-password">
            </div>
            <button class="btn" type="submit">Створити обліковий запис</button>
        </form>
    </div>
`)
