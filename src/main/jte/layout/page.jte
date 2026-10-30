@import gg.jte.Content
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param String title
@param Content content
@param String section = ""
<!DOCTYPE html>
<html lang="uk">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} · Портал ОСС НаУКМА</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<header class="site-header">
    <div class="bar">
        <a class="brand" href="/">Портал ОСС<small>НаУКМА · СК та КС</small></a>
        <nav class="nav">
            <a href="/" class="${section.equals("home") ? "active" : ""}">Головна</a>
            <a href="/orgs/sk" class="${section.equals("sk") ? "active" : ""}">Студентська колегія</a>
            <a href="/orgs/ks" class="${section.equals("ks") ? "active" : ""}">Конгрегація студентів</a>
            @if(ctx.isAuthenticated() && ctx.canPetition())
                <a href="/petitions/mine" class="${section.equals("petitions") ? "active" : ""}">Мої петиції</a>
            @endif
            @for(var org : ctx.editableOrgs())
                <a href="/cabinet/${org.code().name().toLowerCase()}" class="${section.equals("cabinet-" + org.code().name().toLowerCase()) ? "active" : ""}">Кабінет ${org.code().name()}</a>
            @endfor
            @if(ctx.isAdmin())
                <a href="/admin" class="${section.equals("admin") ? "active" : ""}">Адміністрування</a>
            @endif
        </nav>
        <div class="userbox">
            @if(ctx.isAuthenticated())
                <span>${ctx.user().fullName()}</span>
                <form method="post" action="/logout">
                    <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                    <button class="btn secondary small" type="submit">Вийти</button>
                </form>
            @else
                <a class="btn secondary small" href="/login">Увійти</a>
            @endif
        </div>
    </div>
</header>
<main class="container">
    @if(ctx.flashSuccess() != null)
        <div class="flash success" role="status">${ctx.flashSuccess()}</div>
    @endif
    @if(ctx.flashError() != null)
        <div class="flash error" role="alert">${ctx.flashError()}</div>
    @endif
    ${content}
</main>
<footer class="site-footer">Портал студентського самоврядування НаУКМА</footer>
</body>
</html>
