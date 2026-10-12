@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.meeting.MeetingDto
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.protocol.ProtocolDto
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param OrganizationDto org
@param List<MeetingDto> meetings
@param Map<Long, ProtocolDto> protocols
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Засідання " + org.code().name(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "meetings")
    <section class="card">
        <h2>Запланувати засідання</h2>
        <form method="post" action="/cabinet/${code}/meetings">
            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
            <div class="grid two">
                <div class="field"><label for="title">Назва</label><input id="title" name="title" type="text" required></div>
                <div class="field"><label for="scheduledAt">Дата й час</label><input id="scheduledAt" name="scheduledAt" type="datetime-local" required></div>
            </div>
            <div class="field"><label for="streamUrl">Посилання на трансляцію (необов’язково)</label><input id="streamUrl" name="streamUrl" type="url"></div>
            <button class="btn" type="submit">Запланувати й анонсувати</button>
        </form>
    </section>
    <section class="section">
        <h2>Усі засідання</h2>
        @if(meetings.isEmpty())
            <div class="empty">Засідань ще немає.</div>
        @else
            <div class="card table-wrap">
                <table class="data">
                    <thead><tr><th>Засідання</th><th>Час</th><th>Статус</th><th>Протокол</th></tr></thead>
                    <tbody>
                    @for(var m : meetings)
                        <tr>
                            <td><a href="/cabinet/${code}/meetings/${m.id()}">${m.title()}</a></td>
                            <td>${ctx.fmt().dateTime(m.scheduledAt())}</td>
                            <td>@template.components.chip(css = Labels.meetingCss(m.status()), text = Labels.meetingStatus(m.status()))</td>
                            <td>
                                @if(protocols.containsKey(m.id()))
                                    <a href="/cabinet/${code}/protocols/${protocols.get(m.id()).id()}">@template.components.chip(css = Labels.protocolCss(protocols.get(m.id()).status()), text = Labels.protocolStatus(protocols.get(m.id()).status()))</a>
                                @else
                                    <span class="muted">—</span>
                                @endif
                            </td>
                        </tr>
                    @endfor
                    </tbody>
                </table>
            </div>
        @endif
    </section>
`)
