@import java.util.List
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.OrgSummary
@param PageContext ctx
@param List<OrgSummary> summaries
@template.layout.page(ctx = ctx, title = "Головна", section = "home", content = @`
    <div class="hero">
        <h1>Портал ОСС НаУКМА</h1>
        <p>Склад, засідання, петиції, протоколи й голосування Студентської колегії та Конгрегації студентів — відкрито для всіх.</p>
    </div>
    <div class="grid two">
        @for(var s : summaries)
            !{String code = s.org().code().name().toLowerCase();}
            <section class="card org-${code}">
                <div class="card-head">
                    <h2 style="margin:0"><a href="/orgs/${code}">${s.org().name()}</a></h2>
                    <span class="chip ${code}">${s.org().code().name()}</span>
                </div>
                <p class="muted small">Анонси публікуються у ${Labels.messenger(s.org().messenger())}.</p>
                <dl class="kv">
                    <dt>Склад</dt><dd>${s.memberCount()} осіб</dd>
                    <dt>Наступне засідання</dt>
                    <dd>
                        @if(s.nextMeeting() != null)
                            <a href="/meetings/${s.nextMeeting().id()}">${s.nextMeeting().title()}</a>,
                            ${ctx.fmt().dateTime(s.nextMeeting().scheduledAt())}
                        @else
                            <span class="muted">не заплановано</span>
                        @endif
                    </dd>
                </dl>
                <p style="margin:1rem 0 0"><a class="btn secondary small" href="/orgs/${code}">Календар засідань</a></p>
            </section>
        @endfor
    </div>
    <section class="section card">
        <div class="row between">
            <div>
                <h2 style="margin:0">Маєте пропозицію?</h2>
                <p class="muted" style="margin:.25rem 0 0">Студенти, які не входять до складу СК чи КС, можуть подати петицію до будь-якого з органів.</p>
            </div>
            @if(ctx.canPetition())
                <a class="btn" href="/petitions/new">Подати петицію</a>
            @endif
        </div>
    </section>
`)
