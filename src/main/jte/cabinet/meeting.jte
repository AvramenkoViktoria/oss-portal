@import java.util.List
@import ua.edu.ukma.oss.meeting.MeetingDto
@import ua.edu.ukma.oss.meeting.MeetingStatus
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.petition.PetitionDto
@import ua.edu.ukma.oss.protocol.ProtocolDto
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param OrganizationDto org
@param MeetingDto meeting
@param boolean started
@param List<PetitionDto> petitions
@param ProtocolDto protocol = null
!{String code = org.code().name().toLowerCase();}
!{boolean planned = meeting.status() == MeetingStatus.PLANNED;}
@template.layout.page(ctx = ctx, title = meeting.title(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "meetings")
    <div class="page-head">
        <div>
            <h1>${meeting.title()}</h1>
            <div class="row">
                @template.components.chip(css = Labels.meetingCss(meeting.status()), text = Labels.meetingStatus(meeting.status()))
                <span class="muted">${ctx.fmt().dateTime(meeting.scheduledAt())}</span>
                <a class="small" href="/meetings/${meeting.id()}">публічна сторінка</a>
            </div>
        </div>
    </div>

    <div class="split">
        <div>
            <section class="card">
                <h2>Порядок денний</h2>
                @if(meeting.agenda().isEmpty())
                    <p class="muted">Пунктів ще немає.</p>
                @else
                    <ol>
                        @for(var item : meeting.agenda())
                            <li>${item.title()}</li>
                        @endfor
                    </ol>
                @endif
                @if(planned)
                    <form method="post" action="/cabinet/${code}/meetings/${meeting.id()}/agenda" class="row">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <input name="title" type="text" placeholder="Новий пункт порядку денного" required style="flex:1;min-width:14rem">
                        <button class="btn secondary" type="submit">Додати пункт</button>
                    </form>
                @endif
            </section>

            @if(planned)
                <section class="card">
                    <h2>Параметри засідання</h2>
                    <form method="post" action="/cabinet/${code}/meetings/${meeting.id()}/update">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <div class="grid two">
                            <div class="field"><label for="title">Назва</label><input id="title" name="title" type="text" value="${meeting.title()}" required></div>
                            <div class="field"><label for="scheduledAt">Дата й час</label><input id="scheduledAt" name="scheduledAt" type="datetime-local" value="${ctx.fmt().inputValue(meeting.scheduledAt())}" required></div>
                        </div>
                        <div class="field"><label for="streamUrl">Трансляція</label><input id="streamUrl" name="streamUrl" type="url" value="${meeting.streamUrl() == null ? "" : meeting.streamUrl()}"></div>
                        <button class="btn secondary" type="submit">Зберегти</button>
                    </form>
                </section>
            @endif
        </div>

        <aside>
            <section class="card">
                <h3>Життєвий цикл засідання</h3>
                @if(planned)
                    @if(started)
                        <p class="muted">Засідання розпочалося. Позначте його проведеним — для нього автоматично з’явиться чернетка протоколу.</p>
                        <form method="post" action="/cabinet/${code}/meetings/${meeting.id()}/held">
                            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                            <button class="btn success" type="submit">Позначити проведеним</button>
                        </form>
                    @else
                        <p class="muted">Позначити засідання проведеним можна після його початку.</p>
                        <button class="btn success" disabled>Позначити проведеним</button>
                    @endif
                    <form method="post" action="/cabinet/${code}/meetings/${meeting.id()}/cancel" style="margin-top:.75rem" onsubmit="return confirm('Скасувати засідання?')">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <button class="btn danger" type="submit">Скасувати засідання</button>
                    </form>
                @elseif(meeting.status() == MeetingStatus.HELD)
                    <p style="margin:0">Засідання відбулося.</p>
                @else
                    <p class="muted">Засідання скасовано; прив’язані петиції повернулися в чергу.</p>
                @endif
            </section>
            @if(protocol != null)
                <section class="card">
                    <h3>Протокол</h3>
                    <p>@template.components.chip(css = Labels.protocolCss(protocol.status()), text = Labels.protocolStatus(protocol.status()))</p>
                    <a class="btn" href="/cabinet/${code}/protocols/${protocol.id()}">Відкрити протокол</a>
                </section>
            @endif
            @if(!petitions.isEmpty())
                <section class="card">
                    <h3>Петиції</h3>
                    <ul class="list">
                        @for(var p : petitions)
                            <li>${p.title()}<div>@template.components.chip(css = Labels.petitionCss(p.status()), text = Labels.petitionStatus(p.status()))</div></li>
                        @endfor
                    </ul>
                </section>
            @endif
        </aside>
    </div>
`)
