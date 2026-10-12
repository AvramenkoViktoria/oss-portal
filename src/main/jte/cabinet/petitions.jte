@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.meeting.MeetingDto
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.petition.PetitionDto
@import ua.edu.ukma.oss.petition.PetitionStatus
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param OrganizationDto org
@param List<PetitionDto> petitions
@param Map<Long, String> authors
@param List<MeetingDto> planned
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Петиції " + org.code().name(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "petitions")
    @if(petitions.isEmpty())
        <div class="empty">Петицій ще не надходило.</div>
    @endif
    @for(var p : petitions)
        <article class="card">
            <div class="card-head">
                <h3 style="margin:0">${p.title()}</h3>
                @template.components.chip(css = Labels.petitionCss(p.status()), text = Labels.petitionStatus(p.status()))
            </div>
            <p class="muted small">${authors.getOrDefault(p.authorId(), "—")} · ${ctx.fmt().date(p.createdAt())}</p>
            <p>${p.text()}</p>
            @if(p.meetingId() != null)
                <p class="small">Розгляд: <a href="/cabinet/${code}/meetings/${p.meetingId()}">засідання</a> (${Labels.petitionLink(p.link())})</p>
            @endif
            @if(p.rejectionReason() != null)
                <p class="small"><strong>Причина відхилення:</strong> ${p.rejectionReason()}</p>
            @endif
            @if(p.status() == PetitionStatus.SUBMITTED)
                <div class="grid three">
                    <details>
                        <summary class="btn secondary small">У порядок денний</summary>
                        @if(planned.isEmpty())
                            <p class="muted small">Немає запланованих засідань.</p>
                        @else
                            <form method="post" action="/cabinet/${code}/petitions/${p.id()}/agenda" style="margin-top:.5rem">
                                <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                                <div class="field">
                                    <select name="meetingId">
                                        @for(var m : planned)
                                            <option value="${m.id()}">${m.title()} — ${ctx.fmt().shortDateTime(m.scheduledAt())}</option>
                                        @endfor
                                    </select>
                                </div>
                                <button class="btn small" type="submit">Додати</button>
                            </form>
                        @endif
                    </details>
                    <details>
                        <summary class="btn secondary small">Окреме засідання</summary>
                        <form method="post" action="/cabinet/${code}/petitions/${p.id()}/separate" style="margin-top:.5rem">
                            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                            <div class="field"><input name="title" type="text" placeholder="Назва засідання" value="Розгляд петиції: ${p.title()}" required></div>
                            <div class="field"><input name="scheduledAt" type="datetime-local" required></div>
                            <button class="btn small" type="submit">Призначити</button>
                        </form>
                    </details>
                    <details>
                        <summary class="btn danger small">Відхилити</summary>
                        <form method="post" action="/cabinet/${code}/petitions/${p.id()}/reject" style="margin-top:.5rem">
                            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                            <div class="field"><textarea name="reason" placeholder="Причина відхилення" style="min-height:4rem" required></textarea></div>
                            <button class="btn danger small" type="submit">Відхилити петицію</button>
                        </form>
                    </details>
                </div>
            @endif
        </article>
    @endfor
`)
