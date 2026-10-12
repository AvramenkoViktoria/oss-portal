@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.notification.OutboxEntryDto
@import ua.edu.ukma.oss.notification.OutboxStatus
@import ua.edu.ukma.oss.notification.StubMessageDto
@import ua.edu.ukma.oss.organization.Messenger
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param List<OutboxEntryDto> entries
@param Map<Long, OrganizationDto> orgs
@param List<StubMessageDto> delivered
@param Map<Messenger, Boolean> outages
@template.layout.page(ctx = ctx, title = "Сповіщення", section = "admin", content = @`
    @template.components.adminHead(active = "notifications")
    <div class="grid two">
        <section class="card">
            <h2>Месенджери (заглушки)</h2>
            <p class="muted small">WhatsApp і Telegram замінено заглушками: повідомлення не йдуть у мережу, а потрапляють у журнал нижче. Імітація збою дозволяє перевірити повторні спроби.</p>
            @for(var m : Messenger.values())
                <div class="row between" style="padding:.4rem 0">
                    <span><strong>${Labels.messenger(m)}</strong>
                        @if(outages.getOrDefault(m, false))<span class="chip danger">збій</span>@else<span class="chip published">працює</span>@endif
                    </span>
                    <form class="inline-form" method="post" action="/admin/notifications/outage">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <input type="hidden" name="messenger" value="${m.name()}">
                        <input type="hidden" name="outage" value="${!outages.getOrDefault(m, false)}">
                        <button class="btn secondary small" type="submit">${outages.getOrDefault(m, false) ? "Вимкнути збій" : "Імітувати збій"}</button>
                    </form>
                </div>
            @endfor
            <form method="post" action="/admin/notifications/process" style="margin-top:.75rem">
                <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                <button class="btn" type="submit">Обробити чергу зараз</button>
            </form>
        </section>
        <section class="card">
            <h2>Доставлені повідомлення</h2>
            @if(delivered.isEmpty())
                <p class="muted">Поки нічого не доставлено.</p>
            @endif
            @for(var d : delivered)
                <div class="stub-msg">
                    <div class="muted small">${Labels.messenger(d.messenger())} · ${d.channelId()} · ${ctx.fmt().shortDateTime(d.sentAt())}</div>
                    ${d.text()}
                </div>
            @endfor
        </section>
    </div>
    <section class="section">
        <h2>Черга розсилки</h2>
        @if(entries.isEmpty())
            <div class="empty">Черга порожня.</div>
        @else
            <div class="card table-wrap">
                <table class="data">
                    <thead><tr><th>Орган</th><th>Повідомлення</th><th>Статус</th><th>Спроби</th><th></th></tr></thead>
                    <tbody>
                    @for(var e : entries)
                        <tr>
                            <td>${orgs.get(e.orgId()).code().name()}</td>
                            <td style="white-space:pre-line">${e.payload()}@if(e.lastError() != null)<div class="small" style="color:var(--danger)">${e.lastError()}</div>@endif</td>
                            <td>@template.components.chip(css = Labels.outboxCss(e.status()), text = Labels.outboxStatus(e.status()))</td>
                            <td>${e.attempts()}</td>
                            <td class="right">
                                @if(e.status() == OutboxStatus.FAILED)
                                    <form class="inline-form" method="post" action="/admin/notifications/${e.id()}/requeue">
                                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                                        <button class="btn secondary small" type="submit">Повторити</button>
                                    </form>
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
