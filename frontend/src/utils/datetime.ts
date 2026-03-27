import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(timezone)

export const BUSINESS_TZ = 'Asia/Shanghai'

export function toBusinessMs(input: string | number | Date) {
  if (typeof input === 'number') return input
  if (input instanceof Date) return input.getTime()
  return dayjs.tz(input, BUSINESS_TZ).valueOf()
}

export function toBusinessDate(input: string | number | Date) {
  return new Date(toBusinessMs(input))
}

export function formatBusinessDateTime(input: string | number | Date, pattern = 'YYYY-MM-DD HH:mm') {
  return dayjs(toBusinessMs(input)).tz(BUSINESS_TZ).format(pattern)
}

export function formatBusinessTime(input: string | number | Date) {
  return formatBusinessDateTime(input, 'HH:mm')
}

export function formatRange(start: string | number | Date, end: string | number | Date) {
  return `${formatBusinessDateTime(start)} - ${formatBusinessDateTime(end, 'HH:mm')}`
}

export function isPast(input: string | number | Date) {
  return toBusinessMs(input) < Date.now()
}

export function businessDayStartMs(date: string) {
  return dayjs.tz(`${date} 00:00:00`, BUSINESS_TZ).valueOf()
}

export function businessDayEndMs(date: string) {
  return dayjs.tz(`${date} 23:59:59.999`, BUSINESS_TZ).valueOf()
}

export function msToBusinessValueFormat(ms: number) {
  return dayjs(ms).tz(BUSINESS_TZ).format('YYYY-MM-DDTHH:mm:ss')
}
