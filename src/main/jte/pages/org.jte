@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.OrgPage
@param PageContext ctx
@param OrgPage page
@param String view = "calendar"
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
        <h2>Засідання</h2>
        <div class="card">
            @template.components.calendar(ctx = ctx, cal = page.calendar(), navPath = "/orgs/" + code, meetingPath = "/meetings/", view = view)
        </div>
        @if(!page.upcoming().isEmpty())
            <h3 style="margin-top:1rem">Найближчі</h3>
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
        <h2>Петиції</h2>
        @if(page.petitions().isEmpty())
            <div class="empty">Петицій ще не надходило.</div>
        @else
            <div class="card">
                <ul class="list">
                    @for(var p : page.petitions())
                        <li class="row between">
                            <div>
                                <a href="/petitions/${p.id()}"><strong>${p.title()}</strong></a>
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
