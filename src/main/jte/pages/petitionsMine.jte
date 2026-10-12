@import java.util.List
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.PetitionRow
@param PageContext ctx
@param List<PetitionRow> rows
@template.layout.page(ctx = ctx, title = "Мої петиції", section = "petitions", content = @`
    <div class="page-head">
        <h1>Мої петиції</h1>
        <a class="btn" href="/petitions/new">Нова петиція</a>
    </div>
    @if(rows.isEmpty())
        <div class="empty">Ви ще не подавали петицій.</div>
    @else
        <div class="grid">
            @for(var r : rows)
                <article class="card">
                    <div class="card-head">
                        <h3 style="margin:0">${r.petition().title()}</h3>
                        @template.components.chip(css = Labels.petitionCss(r.petition().status()), text = Labels.petitionStatus(r.petition().status()))
                    </div>
                    <p class="muted small">${r.org().name()} · подано ${ctx.fmt().date(r.petition().createdAt())}</p>
                    <p>${r.petition().text()}</p>
                    @if(r.petition().meetingId() != null)
                        <p class="small" style="margin:0">
                            Розгляд: <a href="/meetings/${r.petition().meetingId()}">${r.meetingTitle()}</a>
                            (${Labels.petitionLink(r.petition().link())})
                        </p>
                    @endif
                    @if(r.petition().rejectionReason() != null)
                        <p class="small" style="margin:.4rem 0 0"><strong>Причина відхилення:</strong> ${r.petition().rejectionReason()}</p>
                    @endif
                </article>
            @endfor
        </div>
    @endif
`)
