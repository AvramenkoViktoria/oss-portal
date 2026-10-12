@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.OrgPage
@param PageContext ctx
@param OrgPage page
!{String code = page.org().code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = page.org().name(), section = code, content = @`
    <div class="page-head">
        <div>
            <h1>${page.org().name()}</h1>
            <p class="muted" style="margin:0">Анонси та протоколи публікуються у ${Labels.messenger(page.org().messenger())}.</p>
        </div>
        <a class="btn" href="/petitions/new?org=${code}">Подати петицію</a>
    </div>

    <section class="section">
        <h2>Склад</h2>
        @if(page.members().isEmpty())
            <div class="empty">Склад ще не внесено.</div>
        @else
            <div class="card table-wrap">
                <table class="data">
                    <thead><tr><th>Ім’я</th><th>Факультет</th><th>Посада</th><th>Каденція</th></tr></thead>
                    <tbody>
                    @for(var m : page.members())
                        <tr>
                            <td>${m.name()}</td>
                            <td>${m.faculty()}</td>
                            <td>${m.position()}</td>
                            <td>${ctx.fmt().date(m.termStart())} — @if(m.termEnd() != null)${ctx.fmt().date(m.termEnd())}@else<span class="muted">за чинністю</span>@endif</td>
                        </tr>
                    @endfor
                    </tbody>
                </table>
            </div>
        @endif
    </section>

    <section class="section">
        <h2>Найближчі засідання</h2>
        @if(page.upcoming().isEmpty())
            <div class="empty">Засідань не заплановано.</div>
        @else
            <div class="card">
                <ul class="list">
                    @for(var m : page.upcoming())
                        <li class="row between">
                            <div>
                                <a href="/meetings/${m.id()}"><strong>${m.title()}</strong></a>
                                <div class="muted small">${ctx.fmt().dateTime(m.scheduledAt())}</div>
                            </div>
                            @if(m.streamUrl() != null)
                                <a class="btn secondary small" href="${m.streamUrl()}" rel="noopener" target="_blank">Трансляція</a>
                            @endif
                        </li>
                    @endfor
                </ul>
            </div>
        @endif
    </section>

    <section class="section">
        <h2>Протоколи</h2>
        @if(page.protocols().isEmpty())
            <div class="empty">Опублікованих протоколів поки немає.</div>
        @else
            <div class="card">
                <ul class="list">
                    @for(var e : page.protocols())
                        <li class="row between">
                            <div>
                                <a href="/protocols/${e.protocol().id()}"><strong>${e.meeting().title()}</strong></a>
                                <div class="muted small">засідання ${ctx.fmt().date(e.meeting().scheduledAt())} · опубліковано ${ctx.fmt().date(e.protocol().publishedAt())}</div>
                            </div>
                            @template.components.chip(css = "published", text = "Опубліковано")
                        </li>
                    @endfor
                </ul>
            </div>
        @endif
    </section>

    <section class="section">
        <h2>Минулі засідання</h2>
        @if(page.past().isEmpty())
            <div class="empty">Ще немає проведених засідань.</div>
        @else
            <div class="card">
                <ul class="list">
                    @for(var m : page.past())
                        <li class="row between">
                            <div>
                                <a href="/meetings/${m.id()}">${m.title()}</a>
                                <div class="muted small">${ctx.fmt().dateTime(m.scheduledAt())}</div>
                            </div>
                            <span class="row">
                                @template.components.chip(css = Labels.meetingCss(m.status()), text = Labels.meetingStatus(m.status()))
                                @if(page.publishedByMeeting().containsKey(m.id()))
                                    <a class="small" href="/protocols/${page.publishedByMeeting().get(m.id()).id()}">протокол</a>
                                @endif
                            </span>
                        </li>
                    @endfor
                </ul>
            </div>
        @endif
    </section>

    <section class="section">
        <h2>Петиції</h2>
        @if(page.petitions().isEmpty())
            <div class="empty">Петицій ще не надходило.</div>
        @else
            <div class="card">
                <ul class="list">
                    @for(var p : page.petitions())
                        <li class="row between">
                            <div>
                                <strong>${p.title()}</strong>
                                <div class="muted small">
                                    ${ctx.fmt().date(p.createdAt())}
                                    @if(p.meetingId() != null) · <a href="/meetings/${p.meetingId()}">засідання</a>@endif
                                </div>
                            </div>
                            @template.components.chip(css = Labels.petitionCss(p.status()), text = Labels.petitionStatus(p.status()))
                        </li>
                    @endfor
                </ul>
            </div>
        @endif
    </section>
`)
