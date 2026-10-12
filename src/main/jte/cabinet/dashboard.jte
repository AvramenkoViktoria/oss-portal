@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.meeting.MeetingDto
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.petition.PetitionDto
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.ProtocolEntry
@param PageContext ctx
@param OrganizationDto org
@param List<MeetingDto> toHold
@param List<MeetingDto> upcoming
@param List<PetitionDto> queue
@param Map<Long, String> authors
@param List<ProtocolEntry> inWork
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Кабінет " + org.code().name(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "overview")
    <div class="grid three">
        <div class="card"><div class="stat">${toHold.size()}</div><div class="muted">засідань чекають на позначку «проведено»</div></div>
        <div class="card"><div class="stat">${queue.size()}</div><div class="muted">петицій у черзі</div></div>
        <div class="card"><div class="stat">${inWork.size()}</div><div class="muted">засідань із протоколом у роботі</div></div>
    </div>

    <div class="grid two section">
        <section class="card">
            <div class="card-head"><h2 style="margin:0">Потребує дії</h2></div>
            @if(toHold.isEmpty() && queue.isEmpty() && inWork.isEmpty())
                <p class="muted">Усе в порядку — термінових завдань немає.</p>
            @endif
            @if(!toHold.isEmpty())
                <h3>Засідання відбулися?</h3>
                <ul class="list">
                    @for(var m : toHold)
                        <li class="row between">
                            <span><a href="/cabinet/${code}/meetings/${m.id()}">${m.title()}</a><br><span class="muted small">${ctx.fmt().dateTime(m.scheduledAt())}</span></span>
                            <a class="btn small" href="/cabinet/${code}/meetings/${m.id()}">Відкрити</a>
                        </li>
                    @endfor
                </ul>
            @endif
            @if(!inWork.isEmpty())
                <h3>Протокол потребує уваги</h3>
                <ul class="list">
                    @for(var e : inWork)
                        <li class="row between">
                            <span><a href="/cabinet/${code}/meetings/${e.meeting().id()}">${e.meeting().title()}</a></span>
                            @template.components.chip(css = Labels.protocolCss(e.protocol().status()), text = Labels.protocolStatus(e.protocol().status()))
                        </li>
                    @endfor
                </ul>
            @endif
            @if(!queue.isEmpty())
                <h3>Нові петиції</h3>
                <ul class="list">
                    @for(var p : queue)
                        <li>
                            <strong>${p.title()}</strong>
                            <div class="muted small">${authors.getOrDefault(p.authorId(), "—")} · ${ctx.fmt().date(p.createdAt())}</div>
                        </li>
                    @endfor
                </ul>
                <a class="btn secondary small" href="/cabinet/${code}/petitions">До черги петицій</a>
            @endif
        </section>
        <section class="card">
            <div class="card-head">
                <h2 style="margin:0">Найближчі засідання</h2>
                <a class="btn small" href="/cabinet/${code}/meetings">Усі засідання</a>
            </div>
            @if(upcoming.isEmpty())
                <p class="muted">Майбутніх засідань немає.</p>
            @else
                <ul class="list">
                    @for(var m : upcoming)
                        <li>
                            <a href="/cabinet/${code}/meetings/${m.id()}">${m.title()}</a>
                            <div class="muted small">${ctx.fmt().dateTime(m.scheduledAt())} · пунктів порядку: ${m.agenda().size()}</div>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    </div>
`)
