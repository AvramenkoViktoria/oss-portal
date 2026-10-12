@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param int code = 500
@param String message = "Щось пішло не так"
@template.layout.page(ctx = ctx, title = "Помилка " + code, content = @`
    <div class="empty" style="padding:3rem 1rem">
        <h1>${code}</h1>
        <p>${message}</p>
        <a class="btn" href="/">На головну</a>
    </div>
`)
