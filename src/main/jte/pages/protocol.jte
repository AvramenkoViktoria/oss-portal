@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.ProtocolPage
@param PageContext ctx
@param ProtocolPage page
!{String code = page.org().code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Протокол: " + page.meeting().title(), section = code, content = @`
    <div class="breadcrumbs"><a href="/orgs/${code}">${page.org().name()}</a> / <a href="/meetings/${page.meeting().id()}">${page.meeting().title()}</a> / Протокол</div>
    <div class="page-head">
        <div>
            <h1>Протокол засідання «${page.meeting().title()}»</h1>
            <div class="row">
                @template.components.chip(css = "published", text = "Опубліковано")
                <span class="muted">засідання ${ctx.fmt().dateTime(page.meeting().scheduledAt())}</span>
            </div>
        </div>
        @if(page.protocol().hasFile())
            <a class="btn" href="/protocols/${page.protocol().id()}/file">Завантажити документ (${page.protocol().fileName()})</a>
        @endif
    </div>

    @template.components.lifecycle(ctx = ctx, page = page)

    <section class="section">
        <h2>Порядок денний</h2>
        @if(page.meeting().agenda().isEmpty())
            <p class="muted">Порядок денний не вносився.</p>
        @else
            <ol>
                @for(var item : page.meeting().agenda())
                    <li>${item.title()}</li>
                @endfor
            </ol>
        @endif
    </section>

    <section class="section">
        <h2>Голосування</h2>
        @if(page.votings().isEmpty())
            <div class="empty">Голосувань у цьому протоколі немає.</div>
        @else
            @for(var v : page.votings())
                @template.components.votingBlock(v = v)
            @endfor
        @endif
    </section>
`)
