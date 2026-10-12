@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.MeetingPage
@param PageContext ctx
@param MeetingPage page
@param boolean canEdit = false
!{String code = page.org().code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = page.meeting().title(), section = code, content = @`
    <div class="breadcrumbs"><a href="/orgs/${code}">${page.org().name()}</a> / Засідання</div>
    <div class="page-head">
        <div>
            <h1>${page.meeting().title()}</h1>
            <div class="row">
                @template.components.chip(css = Labels.meetingCss(page.meeting().status()), text = Labels.meetingStatus(page.meeting().status()))
                <span class="muted">${ctx.fmt().dateTime(page.meeting().scheduledAt())}</span>
            </div>
        </div>
        <div class="row">
            @if(page.meeting().streamUrl() != null)
                <a class="btn secondary" href="${page.meeting().streamUrl()}" rel="noopener" target="_blank">Трансляція</a>
            @endif
            @if(canEdit)
                <a class="btn" href="/cabinet/${code}/meetings/${page.meeting().id()}">Керувати</a>
            @endif
        </div>
    </div>

    <div class="split">
        <section class="card">
            <h2>Порядок денний</h2>
            @if(page.meeting().agenda().isEmpty())
                <p class="muted">Порядок денний ще не сформовано.</p>
            @else
                <ol>
                    @for(var item : page.meeting().agenda())
                        <li>${item.title()}</li>
                    @endfor
                </ol>
            @endif
        </section>
        <aside>
            <section class="card">
                <h3>Протокол</h3>
                @if(page.protocol() != null)
                    <p><a class="btn success" href="/protocols/${page.protocol().id()}">Відкрити протокол</a></p>
                    <p class="muted small" style="margin:0">Опубліковано ${ctx.fmt().date(page.protocol().publishedAt())}</p>
                @else
                    <p class="muted" style="margin:0">Протокол ще не опубліковано.</p>
                @endif
            </section>
            @if(!page.petitions().isEmpty())
                <section class="card">
                    <h3>Петиції на засіданні</h3>
                    <ul class="list">
                        @for(var p : page.petitions())
                            <li>
                                ${p.title()}
                                <div>@template.components.chip(css = Labels.petitionCss(p.status()), text = Labels.petitionStatus(p.status()))</div>
                            </li>
                        @endfor
                    </ul>
                </section>
            @endif
        </aside>
    </div>
`)
